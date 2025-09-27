import Foundation
import UIKit
import Photos
import PhotosUI
import ImageIO
import CoreLocation

/// Extracts metadata from photos including EXIF data and PHAsset properties
final class PhotoMetadataExtractor {

    // MARK: - Metadata Structure
    struct PhotoMetadata {
        let originalFilename: String?
        let creationDate: Date?
        let modificationDate: Date?
        let pixelWidth: Int
        let pixelHeight: Int
        let fileSize: Int64?
        let fileExtension: String?
        let mimeType: String?

        // Camera information
        let cameraMake: String?
        let cameraModel: String?
        let lensMake: String?
        let lensModel: String?

        // Capture settings
        let iso: Int?
        let aperture: Double?
        let shutterSpeed: Double?
        let focalLength: Double?
        let flash: Bool?

        // Location
        let location: CLLocation?

        // Orientation
        let orientation: UIImage.Orientation

        // Color information
        let colorSpace: String?
        let bitDepth: Int?

        // Additional flags
        let isHDR: Bool
        let isBurst: Bool
        let isLivePhoto: Bool
        let isEdited: Bool
    }

    // MARK: - Public Methods

    /// Extract metadata from PHPickerResult and UIImage
    func extractMetadata(from result: PHPickerResult, image: UIImage) -> PhotoMetadata {
        var metadata = extractBasicMetadata(from: image)

        // Try to get additional metadata from asset identifier
        if let assetIdentifier = result.assetIdentifier {
            metadata = enrichWithPHAssetData(metadata, identifier: assetIdentifier)
        }

        // Extract filename from item provider
        if let suggestedName = result.itemProvider.suggestedName {
            metadata = PhotoMetadata(
                originalFilename: suggestedName,
                creationDate: metadata.creationDate,
                modificationDate: metadata.modificationDate,
                pixelWidth: metadata.pixelWidth,
                pixelHeight: metadata.pixelHeight,
                fileSize: metadata.fileSize,
                fileExtension: extractFileExtension(from: suggestedName),
                mimeType: metadata.mimeType,
                cameraMake: metadata.cameraMake,
                cameraModel: metadata.cameraModel,
                lensMake: metadata.lensMake,
                lensModel: metadata.lensModel,
                iso: metadata.iso,
                aperture: metadata.aperture,
                shutterSpeed: metadata.shutterSpeed,
                focalLength: metadata.focalLength,
                flash: metadata.flash,
                location: metadata.location,
                orientation: metadata.orientation,
                colorSpace: metadata.colorSpace,
                bitDepth: metadata.bitDepth,
                isHDR: metadata.isHDR,
                isBurst: metadata.isBurst,
                isLivePhoto: metadata.isLivePhoto,
                isEdited: metadata.isEdited
            )
        }

        return metadata
    }

    /// Extract metadata from PHAsset
    func extractMetadata(from asset: PHAsset) -> PhotoMetadata {
        return PhotoMetadata(
            originalFilename: getAssetFilename(asset),
            creationDate: asset.creationDate,
            modificationDate: asset.modificationDate,
            pixelWidth: asset.pixelWidth,
            pixelHeight: asset.pixelHeight,
            fileSize: nil, // Not directly available from PHAsset
            fileExtension: getAssetFileExtension(asset),
            mimeType: nil,
            cameraMake: nil,
            cameraModel: nil,
            lensMake: nil,
            lensModel: nil,
            iso: nil,
            aperture: nil,
            shutterSpeed: nil,
            focalLength: nil,
            flash: nil,
            location: asset.location,
            orientation: .up,
            colorSpace: nil,
            bitDepth: nil,
            isHDR: asset.mediaSubtypes.contains(.photoHDR),
            isBurst: asset.representsBurst,
            isLivePhoto: asset.mediaSubtypes.contains(.photoLive),
            isEdited: asset.hasAdjustments
        )
    }

    /// Extract metadata from image data
    func extractMetadata(from imageData: Data) -> PhotoMetadata? {
        guard let imageSource = CGImageSourceCreateWithData(imageData as CFData, nil) else {
            return nil
        }

        guard let properties = CGImageSourceCopyPropertiesAtIndex(imageSource, 0, nil) as? [String: Any] else {
            return nil
        }

        // Extract EXIF data
        let exifData = properties[kCGImagePropertyExifDictionary as String] as? [String: Any]
        let tiffData = properties[kCGImagePropertyTIFFDictionary as String] as? [String: Any]
        let gpsData = properties[kCGImagePropertyGPSDictionary as String] as? [String: Any]

        // Extract dimensions
        let pixelWidth = properties[kCGImagePropertyPixelWidth as String] as? Int ?? 0
        let pixelHeight = properties[kCGImagePropertyPixelHeight as String] as? Int ?? 0

        // Extract dates
        let creationDate = extractDate(from: exifData?[kCGImagePropertyExifDateTimeOriginal as String])
            ?? extractDate(from: tiffData?[kCGImagePropertyTIFFDateTime as String])

        // Extract camera info
        let cameraMake = tiffData?[kCGImagePropertyTIFFMake as String] as? String
        let cameraModel = tiffData?[kCGImagePropertyTIFFModel as String] as? String

        // Extract capture settings
        let iso = exifData?[kCGImagePropertyExifISOSpeedRatings as String] as? [Int]
        let aperture = exifData?[kCGImagePropertyExifFNumber as String] as? Double
        let shutterSpeed = exifData?[kCGImagePropertyExifExposureTime as String] as? Double
        let focalLength = exifData?[kCGImagePropertyExifFocalLength as String] as? Double
        let flash = extractFlashInfo(from: exifData?[kCGImagePropertyExifFlash as String])

        // Extract orientation
        let orientationValue = properties[kCGImagePropertyOrientation as String] as? Int ?? 1
        let orientation = UIImage.Orientation(rawValue: orientationValue - 1) ?? .up

        // Extract color space
        let colorSpace = properties[kCGImagePropertyColorModel as String] as? String
        let bitDepth = properties[kCGImagePropertyDepth as String] as? Int

        // Extract location
        let location = extractLocation(from: gpsData)

        return PhotoMetadata(
            originalFilename: nil,
            creationDate: creationDate,
            modificationDate: nil,
            pixelWidth: pixelWidth,
            pixelHeight: pixelHeight,
            fileSize: Int64(imageData.count),
            fileExtension: nil,
            mimeType: nil,
            cameraMake: cameraMake,
            cameraModel: cameraModel,
            lensMake: exifData?[kCGImagePropertyExifLensMake as String] as? String,
            lensModel: exifData?[kCGImagePropertyExifLensModel as String] as? String,
            iso: iso?.first,
            aperture: aperture,
            shutterSpeed: shutterSpeed,
            focalLength: focalLength,
            flash: flash,
            location: location,
            orientation: orientation,
            colorSpace: colorSpace,
            bitDepth: bitDepth,
            isHDR: false,
            isBurst: false,
            isLivePhoto: false,
            isEdited: false
        )
    }

    // MARK: - Private Methods

    private func extractBasicMetadata(from image: UIImage) -> PhotoMetadata {
        let pixelWidth = Int(image.size.width * image.scale)
        let pixelHeight = Int(image.size.height * image.scale)

        return PhotoMetadata(
            originalFilename: nil,
            creationDate: Date(),
            modificationDate: nil,
            pixelWidth: pixelWidth,
            pixelHeight: pixelHeight,
            fileSize: nil,
            fileExtension: "jpg",
            mimeType: "image/jpeg",
            cameraMake: nil,
            cameraModel: nil,
            lensMake: nil,
            lensModel: nil,
            iso: nil,
            aperture: nil,
            shutterSpeed: nil,
            focalLength: nil,
            flash: nil,
            location: nil,
            orientation: image.imageOrientation,
            colorSpace: nil,
            bitDepth: nil,
            isHDR: false,
            isBurst: false,
            isLivePhoto: false,
            isEdited: false
        )
    }

    private func enrichWithPHAssetData(_ metadata: PhotoMetadata, identifier: String) -> PhotoMetadata {
        // Try to fetch PHAsset using identifier
        let fetchResult = PHAsset.fetchAssets(withLocalIdentifiers: [identifier], options: nil)

        guard let asset = fetchResult.firstObject else {
            return metadata
        }

        return PhotoMetadata(
            originalFilename: metadata.originalFilename ?? getAssetFilename(asset),
            creationDate: asset.creationDate ?? metadata.creationDate,
            modificationDate: asset.modificationDate ?? metadata.modificationDate,
            pixelWidth: asset.pixelWidth,
            pixelHeight: asset.pixelHeight,
            fileSize: metadata.fileSize,
            fileExtension: metadata.fileExtension ?? getAssetFileExtension(asset),
            mimeType: metadata.mimeType,
            cameraMake: metadata.cameraMake,
            cameraModel: metadata.cameraModel,
            lensMake: metadata.lensMake,
            lensModel: metadata.lensModel,
            iso: metadata.iso,
            aperture: metadata.aperture,
            shutterSpeed: metadata.shutterSpeed,
            focalLength: metadata.focalLength,
            flash: metadata.flash,
            location: asset.location ?? metadata.location,
            orientation: metadata.orientation,
            colorSpace: metadata.colorSpace,
            bitDepth: metadata.bitDepth,
            isHDR: asset.mediaSubtypes.contains(.photoHDR),
            isBurst: asset.representsBurst,
            isLivePhoto: asset.mediaSubtypes.contains(.photoLive),
            isEdited: asset.hasAdjustments
        )
    }

    private func getAssetFilename(_ asset: PHAsset) -> String? {
        let resources = PHAssetResource.assetResources(for: asset)
        return resources.first?.originalFilename
    }

    private func getAssetFileExtension(_ asset: PHAsset) -> String? {
        let resources = PHAssetResource.assetResources(for: asset)
        guard let filename = resources.first?.originalFilename else { return nil }
        return extractFileExtension(from: filename)
    }

    private func extractFileExtension(from filename: String) -> String? {
        guard let lastDot = filename.lastIndex(of: ".") else { return nil }
        let extensionIndex = filename.index(after: lastDot)
        guard extensionIndex < filename.endIndex else { return nil }
        return String(filename[extensionIndex...]).lowercased()
    }

    private func extractDate(from value: Any?) -> Date? {
        guard let dateString = value as? String else { return nil }

        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy:MM:dd HH:mm:ss"
        return formatter.date(from: dateString)
    }

    private func extractFlashInfo(from value: Any?) -> Bool? {
        guard let flashValue = value as? Int else { return nil }
        // Flash fired if odd number (bit 0 is set)
        return (flashValue & 0x01) != 0
    }

    private func extractLocation(from gpsData: [String: Any]?) -> CLLocation? {
        guard let gpsData = gpsData else { return nil }

        guard let latitude = gpsData[kCGImagePropertyGPSLatitude as String] as? Double,
              let latitudeRef = gpsData[kCGImagePropertyGPSLatitudeRef as String] as? String,
              let longitude = gpsData[kCGImagePropertyGPSLongitude as String] as? Double,
              let longitudeRef = gpsData[kCGImagePropertyGPSLongitudeRef as String] as? String else {
            return nil
        }

        let lat = latitudeRef == "S" ? -latitude : latitude
        let lon = longitudeRef == "W" ? -longitude : longitude

        let altitude = gpsData[kCGImagePropertyGPSAltitude as String] as? Double

        return CLLocation(
            coordinate: CLLocationCoordinate2D(latitude: lat, longitude: lon),
            altitude: altitude ?? 0,
            horizontalAccuracy: 0,
            verticalAccuracy: 0,
            timestamp: Date()
        )
    }
}
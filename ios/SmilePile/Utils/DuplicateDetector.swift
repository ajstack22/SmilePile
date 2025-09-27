import Foundation
import CryptoKit
import UIKit
import CoreImage

/// Detects duplicate photos using various hashing techniques
final class DuplicateDetector {

    // MARK: - Configuration
    struct Configuration {
        static let perceptualHashSize = 8 // 8x8 for pHash
        static let colorHistogramBins = 16 // Bins per channel for histogram
        static let similarityThreshold: Float = 0.95 // 95% similarity
    }

    // MARK: - Hash Types
    enum HashType {
        case sha256       // Exact file hash
        case perceptual   // Visual similarity hash
        case average      // Simple average hash
        case difference   // Difference hash
        case colorHistogram // Color distribution hash
    }

    // MARK: - Duplicate Result
    struct DuplicateCheckResult {
        let isDuplicate: Bool
        let similarity: Float
        let matchingHash: String?
        let hashType: HashType
    }

    // MARK: - Properties
    private var hashCache: [String: Set<String>] = [:]
    private let ciContext: CIContext
    private let processingQueue: DispatchQueue

    // MARK: - Initialization
    init() {
        self.ciContext = CIContext(options: [.useSoftwareRenderer: false])
        self.processingQueue = DispatchQueue(
            label: "com.smilepile.duplicatedetector",
            qos: .userInitiated
        )
    }

    // MARK: - Public Methods

    /// Calculate SHA-256 hash for exact duplicate detection
    func calculateHash(for data: Data) -> String {
        let hash = SHA256.hash(data: data)
        return hash.compactMap { String(format: "%02x", $0) }.joined()
    }

    /// Calculate file hash from URL
    func calculateFileHash(at url: URL) throws -> String {
        let data = try Data(contentsOf: url)
        return calculateHash(for: data)
    }

    /// Check if image data is a duplicate using multiple methods
    func checkForDuplicate(
        imageData: Data,
        against existingHashes: Set<String>,
        usePerceptualHash: Bool = true
    ) async -> DuplicateCheckResult {
        // First, check exact hash
        let exactHash = calculateHash(for: imageData)
        if existingHashes.contains(exactHash) {
            return DuplicateCheckResult(
                isDuplicate: true,
                similarity: 1.0,
                matchingHash: exactHash,
                hashType: .sha256
            )
        }

        // If not exact match and perceptual hashing is enabled
        if usePerceptualHash, let image = UIImage(data: imageData) {
            // Check perceptual hash for visual similarity
            let perceptualResult = await checkPerceptualDuplicate(
                image: image,
                against: existingHashes
            )

            if perceptualResult.isDuplicate {
                return perceptualResult
            }
        }

        return DuplicateCheckResult(
            isDuplicate: false,
            similarity: 0.0,
            matchingHash: nil,
            hashType: .sha256
        )
    }

    /// Generate perceptual hash for visual similarity detection
    func generatePerceptualHash(for image: UIImage) async -> String {
        return await withCheckedContinuation { continuation in
            processingQueue.async { [weak self] in
                guard let self = self else {
                    continuation.resume(returning: "")
                    return
                }

                let hash = self.calculatePerceptualHash(image)
                continuation.resume(returning: hash)
            }
        }
    }

    /// Generate average hash for simple similarity detection
    func generateAverageHash(for image: UIImage) -> String {
        // Resize to 8x8
        let size = CGSize(width: 8, height: 8)
        UIGraphicsBeginImageContext(size)
        defer { UIGraphicsEndImageContext() }

        image.draw(in: CGRect(origin: .zero, size: size))
        guard let resized = UIGraphicsGetImageFromCurrentImageContext(),
              let cgImage = resized.cgImage else {
            return ""
        }

        // Convert to grayscale and calculate average
        let pixels = getGrayscalePixels(from: cgImage)
        let average = pixels.reduce(0, +) / pixels.count

        // Generate hash based on whether pixels are above or below average
        var hash = ""
        for pixel in pixels {
            hash += pixel > average ? "1" : "0"
        }

        return hash
    }

    /// Generate difference hash for gradient-based similarity
    func generateDifferenceHash(for image: UIImage) -> String {
        // Resize to 9x8 (one extra column for differences)
        let size = CGSize(width: 9, height: 8)
        UIGraphicsBeginImageContext(size)
        defer { UIGraphicsEndImageContext() }

        image.draw(in: CGRect(origin: .zero, size: size))
        guard let resized = UIGraphicsGetImageFromCurrentImageContext(),
              let cgImage = resized.cgImage else {
            return ""
        }

        let pixels = getGrayscalePixels(from: cgImage)
        var hash = ""

        // Compare adjacent pixels
        for row in 0..<8 {
            for col in 0..<8 {
                let index = row * 9 + col
                let nextIndex = index + 1
                if index < pixels.count && nextIndex < pixels.count {
                    hash += pixels[index] > pixels[nextIndex] ? "1" : "0"
                }
            }
        }

        return hash
    }

    /// Check if a file has already been imported
    func isFileImported(hash: String, in category: String? = nil) -> Bool {
        let cacheIdentifier = category ?? "all"
        return hashCache[cacheIdentifier]?.contains(hash) ?? false
    }

    /// Add hash to imported set
    func markAsImported(hash: String, category: String? = nil) {
        let cacheIdentifier = category ?? "all"
        if hashCache[cacheIdentifier] == nil {
            hashCache[cacheIdentifier] = Set()
        }
        hashCache[cacheIdentifier]?.insert(hash)
    }

    /// Clear hash cache for a category
    func clearCache(for category: String? = nil) {
        if let category = category {
            hashCache[category] = nil
        } else {
            hashCache.removeAll()
        }
    }

    /// Calculate similarity between two hashes
    func calculateSimilarity(hash1: String, hash2: String) -> Float {
        guard hash1.count == hash2.count else { return 0.0 }

        let matchingBits = zip(hash1, hash2).filter { $0 == $1 }.count
        return Float(matchingBits) / Float(hash1.count)
    }

    // MARK: - Private Methods

    private func checkPerceptualDuplicate(
        image: UIImage,
        against existingHashes: Set<String>
    ) async -> DuplicateCheckResult {
        let perceptualHash = await generatePerceptualHash(for: image)

        // Check against existing perceptual hashes
        for existingHash in existingHashes {
            // Only compare if it looks like a perceptual hash (binary string)
            if existingHash.count == 64 && existingHash.allSatisfy({ $0 == "0" || $0 == "1" }) {
                let similarity = calculateSimilarity(hash1: perceptualHash, hash2: existingHash)

                if similarity >= Configuration.similarityThreshold {
                    return DuplicateCheckResult(
                        isDuplicate: true,
                        similarity: similarity,
                        matchingHash: existingHash,
                        hashType: .perceptual
                    )
                }
            }
        }

        return DuplicateCheckResult(
            isDuplicate: false,
            similarity: 0.0,
            matchingHash: nil,
            hashType: .perceptual
        )
    }

    private func calculatePerceptualHash(_ image: UIImage) -> String {
        // Resize to small size (8x8)
        let size = CGSize(
            width: Configuration.perceptualHashSize,
            height: Configuration.perceptualHashSize
        )

        UIGraphicsBeginImageContext(size)
        defer { UIGraphicsEndImageContext() }

        image.draw(in: CGRect(origin: .zero, size: size))

        guard let resized = UIGraphicsGetImageFromCurrentImageContext(),
              let cgImage = resized.cgImage else {
            return ""
        }

        // Convert to grayscale
        let pixels = getGrayscalePixels(from: cgImage)

        // Apply DCT (simplified - using average for now)
        let average = pixels.reduce(0, +) / pixels.count

        // Generate hash
        var hash = ""
        for pixel in pixels {
            hash += pixel > average ? "1" : "0"
        }

        return hash
    }

    private func getGrayscalePixels(from cgImage: CGImage) -> [Int] {
        let width = cgImage.width
        let height = cgImage.height
        let bytesPerPixel = 1
        let bytesPerRow = bytesPerPixel * width
        let bitsPerComponent = 8

        var pixelData = [UInt8](repeating: 0, count: height * bytesPerRow)

        let colorSpace = CGColorSpaceCreateDeviceGray()
        guard let context = CGContext(
            data: &pixelData,
            width: width,
            height: height,
            bitsPerComponent: bitsPerComponent,
            bytesPerRow: bytesPerRow,
            space: colorSpace,
            bitmapInfo: CGImageAlphaInfo.none.rawValue
        ) else {
            return []
        }

        context.draw(cgImage, in: CGRect(x: 0, y: 0, width: width, height: height))

        return pixelData.map { Int($0) }
    }

    /// Generate color histogram for color-based similarity
    private func generateColorHistogram(for image: UIImage) -> [Float] {
        guard let cgImage = image.cgImage else { return [] }

        let width = cgImage.width
        let height = cgImage.height
        let bytesPerPixel = 4
        let bytesPerRow = bytesPerPixel * width
        let bitsPerComponent = 8

        var pixelData = [UInt8](repeating: 0, count: height * bytesPerRow)

        let colorSpace = CGColorSpaceCreateDeviceRGB()
        guard let context = CGContext(
            data: &pixelData,
            width: width,
            height: height,
            bitsPerComponent: bitsPerComponent,
            bytesPerRow: bytesPerRow,
            space: colorSpace,
            bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue
        ) else {
            return []
        }

        context.draw(cgImage, in: CGRect(x: 0, y: 0, width: width, height: height))

        // Create histogram
        let bins = Configuration.colorHistogramBins
        var histogram = [Float](repeating: 0, count: bins * 3) // RGB channels

        let pixelCount = width * height
        let binSize = 256 / bins

        for y in 0..<height {
            for x in 0..<width {
                let index = (y * width + x) * bytesPerPixel
                let r = Int(pixelData[index]) / binSize
                let g = Int(pixelData[index + 1]) / binSize
                let b = Int(pixelData[index + 2]) / binSize

                histogram[r] += 1
                histogram[bins + g] += 1
                histogram[bins * 2 + b] += 1
            }
        }

        // Normalize histogram
        for i in 0..<histogram.count {
            histogram[i] /= Float(pixelCount)
        }

        return histogram
    }

    /// Compare two color histograms
    private func compareHistograms(_ hist1: [Float], _ hist2: [Float]) -> Float {
        guard hist1.count == hist2.count else { return 0.0 }

        // Calculate correlation coefficient
        let n = hist1.count
        let mean1 = hist1.reduce(0, +) / Float(n)
        let mean2 = hist2.reduce(0, +) / Float(n)

        var numerator: Float = 0
        var denominator1: Float = 0
        var denominator2: Float = 0

        for i in 0..<n {
            let diff1 = hist1[i] - mean1
            let diff2 = hist2[i] - mean2
            numerator += diff1 * diff2
            denominator1 += diff1 * diff1
            denominator2 += diff2 * diff2
        }

        guard denominator1 > 0 && denominator2 > 0 else { return 0.0 }

        return numerator / sqrt(denominator1 * denominator2)
    }
}
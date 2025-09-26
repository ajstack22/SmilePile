#!/usr/bin/env swift

import Photos
import Foundation

print("Testing Photo Library Permission Flow")
print("=====================================")

// Check current permission status
let currentStatus = PHPhotoLibrary.authorizationStatus(for: .readWrite)
print("\nCurrent permission status: \(currentStatus.rawValue)")

switch currentStatus {
case .notDetermined:
    print("Status: Not Determined - User hasn't been asked yet")
case .restricted:
    print("Status: Restricted - Parental controls or device management")
case .denied:
    print("Status: Denied - User explicitly denied access")
case .authorized:
    print("Status: Authorized - Full library access granted")
case .limited:
    print("Status: Limited - User selected specific photos")
@unknown default:
    print("Status: Unknown")
}

// Test permission request (only works when not determined)
if currentStatus == .notDetermined {
    print("\nRequesting photo library permission...")
    let semaphore = DispatchSemaphore(value: 0)

    PHPhotoLibrary.requestAuthorization(for: .readWrite) { status in
        print("New permission status: \(status.rawValue)")
        semaphore.signal()
    }

    semaphore.wait()
}

// Test fetching photos if authorized
if currentStatus == .authorized || currentStatus == .limited {
    print("\nTesting photo fetch...")

    let fetchOptions = PHFetchOptions()
    fetchOptions.sortDescriptors = [NSSortDescriptor(key: "creationDate", ascending: false)]
    fetchOptions.fetchLimit = 10

    let assets = PHAsset.fetchAssets(with: .image, options: fetchOptions)
    print("Found \(assets.count) photos")

    if assets.count > 0 {
        print("\nFirst 5 photos:")
        for i in 0..<min(5, assets.count) {
            let asset = assets[i]
            print("  - Photo \(i+1): \(asset.localIdentifier)")
            print("    Created: \(asset.creationDate ?? Date())")
            print("    Size: \(asset.pixelWidth)x\(asset.pixelHeight)")
        }
    }
}

print("\n✅ Photo permission test complete")
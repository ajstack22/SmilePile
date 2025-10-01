// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "SmilePile",
    platforms: [
        .iOS(.v16)
    ],
    products: [
        .library(
            name: "SmilePile",
            targets: ["SmilePile"]
        ),
    ],
    dependencies: [
        .package(url: "https://github.com/weichsel/ZIPFoundation.git", from: "0.9.0")
    ],
    targets: [
        .target(
            name: "SmilePile",
            dependencies: ["ZIPFoundation"],
            path: "SmilePile",
            exclude: ["Preview Content"]
        ),
        .testTarget(
            name: "SmilePileTests",
            dependencies: ["SmilePile"],
            path: "SmilePileTests"
        ),
    ]
)
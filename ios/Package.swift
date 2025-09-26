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
    dependencies: [],
    targets: [
        .target(
            name: "SmilePile",
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
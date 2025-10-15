fastlane documentation
----

# Installation

Make sure you have the latest version of the Xcode command line tools installed:

```sh
xcode-select --install
```

For _fastlane_ installation instructions, see [Installing _fastlane_](https://docs.fastlane.tools/#installing-fastlane)

# Available Actions

## iOS

### ios qual_ios

```sh
[bundle exec] fastlane ios qual_ios
```

Build QUAL for simulator testing

### ios stage_ios

```sh
[bundle exec] fastlane ios stage_ios
```

Build and upload STAGE to TestFlight Internal Testing

### ios beta_ios

```sh
[bundle exec] fastlane ios beta_ios
```

Build and upload BETA to TestFlight External Testing

### ios prod_ios

```sh
[bundle exec] fastlane ios prod_ios
```

Build and upload PROD to App Store Connect

----

This README.md is auto-generated and will be re-generated every time [_fastlane_](https://fastlane.tools) is run.

More information about _fastlane_ can be found on [fastlane.tools](https://fastlane.tools).

The documentation of _fastlane_ can be found on [docs.fastlane.tools](https://docs.fastlane.tools).

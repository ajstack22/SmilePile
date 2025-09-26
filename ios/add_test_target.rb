#!/usr/bin/env ruby

require 'xcodeproj'
require 'fileutils'

# Open the project
project_path = '/Users/adamstack/SmilePile/ios/SmilePile.xcodeproj'
project = Xcodeproj::Project.open(project_path)

# Check if test target already exists
existing_test_target = project.targets.find { |t| t.name == 'SmilePileTests' }
if existing_test_target
  puts "Test target already exists, removing it first..."
  project.targets.delete(existing_test_target)
end

# Get the main app target
main_target = project.targets.find { |t| t.name == 'SmilePile' }
unless main_target
  puts "Error: Could not find main app target"
  exit 1
end

# Create test target
test_target = project.new_target(:unit_test_bundle, 'SmilePileTests', :ios, '16.0')

# Set up test target configuration
test_target.build_configurations.each do |config|
  config.build_settings['SWIFT_VERSION'] = '5.0'
  config.build_settings['PRODUCT_BUNDLE_IDENTIFIER'] = 'com.smilepile.SmilePileTests'
  config.build_settings['INFOPLIST_FILE'] = 'SmilePileTests/Info.plist'
  config.build_settings['IPHONEOS_DEPLOYMENT_TARGET'] = '16.0'
  config.build_settings['ENABLE_MODULE_VERIFIER'] = 'YES'
  config.build_settings['ENABLE_TESTING_SEARCH_PATHS'] = 'YES'
  config.build_settings['TEST_HOST'] = '$(BUILT_PRODUCTS_DIR)/SmilePile.app/SmilePile'
  config.build_settings['BUNDLE_LOADER'] = '$(TEST_HOST)'
end

# Add main app as dependency
test_target.add_dependency(main_target)

# Create test group if it doesn't exist
test_group = project.main_group['SmilePileTests'] || project.main_group.new_group('SmilePileTests')

# Function to add files recursively
def add_files_to_group(group, path, test_target)
  Dir.glob("#{path}/**/*.swift").each do |file|
    relative_path = file.sub("#{path}/", '')

    # Create nested groups for subdirectories
    dir_parts = File.dirname(relative_path).split('/')
    current_group = group

    unless dir_parts == ['.']
      dir_parts.each do |dir|
        current_group = current_group[dir] || current_group.new_group(dir)
      end
    end

    # Add the file to the appropriate group
    file_ref = current_group.new_file(file)
    test_target.add_file_references([file_ref])
  end
end

# Add test files to the target
test_files_path = '/Users/adamstack/SmilePile/ios/SmilePileTests'
add_files_to_group(test_group, test_files_path, test_target)

# Create Info.plist for tests if it doesn't exist
info_plist_path = "#{test_files_path}/Info.plist"
unless File.exist?(info_plist_path)
  info_plist_content = <<-PLIST
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CFBundleDevelopmentRegion</key>
    <string>$(DEVELOPMENT_LANGUAGE)</string>
    <key>CFBundleExecutable</key>
    <string>$(EXECUTABLE_NAME)</string>
    <key>CFBundleIdentifier</key>
    <string>$(PRODUCT_BUNDLE_IDENTIFIER)</string>
    <key>CFBundleInfoDictionaryVersion</key>
    <string>6.0</string>
    <key>CFBundleName</key>
    <string>$(PRODUCT_NAME)</string>
    <key>CFBundlePackageType</key>
    <string>$(PRODUCT_BUNDLE_PACKAGE_TYPE)</string>
    <key>CFBundleShortVersionString</key>
    <string>1.0</string>
    <key>CFBundleVersion</key>
    <string>1</string>
</dict>
</plist>
  PLIST
  File.write(info_plist_path, info_plist_content)
end

# Add the test target to the main scheme
schemes_path = "#{project_path}/xcshareddata/xcschemes"
FileUtils.mkdir_p(schemes_path) unless Dir.exist?(schemes_path)

scheme_path = "#{schemes_path}/SmilePile.xcscheme"
if File.exist?(scheme_path)
  scheme = Xcodeproj::XCScheme.new(scheme_path)
else
  scheme = Xcodeproj::XCScheme.new
  scheme.add_build_target(main_target)
end

# Add test action to scheme
scheme.test_action.add_testable(Xcodeproj::XCScheme::TestAction::TestableReference.new(test_target))

# Save scheme
scheme.save_as(project_path, 'SmilePile')

# Save the project
project.save

puts "✅ Test target 'SmilePileTests' has been added to the project successfully!"
puts "📝 Created Info.plist for tests at: #{info_plist_path}"
puts "🔧 Updated scheme to include test target"
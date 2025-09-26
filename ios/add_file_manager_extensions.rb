#!/usr/bin/env ruby

require 'xcodeproj'

# Open the project
project_path = '/Users/adamstack/SmilePile/ios/SmilePile.xcodeproj'
project = Xcodeproj::Project.open(project_path)

# Get the main app target
main_target = project.targets.find { |t| t.name == 'SmilePile' }
unless main_target
  puts "Error: Could not find main app target"
  exit 1
end

# Find or create the Utils group
main_group = project.main_group['SmilePile']
utils_group = main_group['Utils'] || main_group.new_group('Utils')

# Add the FileManagerExtensions file
file_path = '/Users/adamstack/SmilePile/ios/SmilePile/Utils/FileManagerExtensions.swift'
file_ref = utils_group.new_file(file_path)
main_target.add_file_references([file_ref])

# Save the project
project.save

puts "✅ FileManagerExtensions.swift has been added to the project!"
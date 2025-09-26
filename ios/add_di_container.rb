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

# Find or create the Core group
main_group = project.main_group['SmilePile']
core_group = main_group['Core'] || main_group.new_group('Core')
di_group = core_group['DI'] || core_group.new_group('DI')

# Add the DIContainer file
file_path = '/Users/adamstack/SmilePile/ios/SmilePile/Core/DI/DIContainer.swift'
file_ref = di_group.new_file(file_path)
main_target.add_file_references([file_ref])

# Save the project
project.save

puts "✅ DIContainer.swift has been added to the project!"
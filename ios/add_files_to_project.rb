#!/usr/bin/env ruby

require 'xcodeproj'
require 'pathname'

# Open the project
project_path = '/Users/adamstack/SmilePile/ios/SmilePile.xcodeproj'
project = Xcodeproj::Project.open(project_path)

# Get the main target
target = project.targets.find { |t| t.name == 'SmilePile' }

# Files to add
files_to_add = [
  '/Users/adamstack/SmilePile/ios/SmilePile/Utils/PhotoIDGenerator.swift',
  '/Users/adamstack/SmilePile/ios/SmilePile/Data/Migration/PhotoIDMigration.swift'
]

# Get the main group
main_group = project.main_group['SmilePile']

# Find or create groups
utils_group = main_group['Utils'] || main_group.new_group('Utils')
migration_group = main_group['Data']['Migration'] || main_group['Data'].new_group('Migration')

files_to_add.each do |file_path|
  file_name = File.basename(file_path)

  # Determine which group to add to
  group = if file_path.include?('Utils')
    utils_group
  else
    migration_group
  end

  # Check if file already exists in project
  existing_file = group.files.find { |f| f.path&.end_with?(file_name) }

  unless existing_file
    # Add file reference
    file_ref = group.new_reference(file_path)
    file_ref.last_known_file_type = 'sourcecode.swift'

    # Add to target
    target.add_file_references([file_ref])

    puts "Added #{file_name} to project"
  else
    puts "#{file_name} already exists in project"
  end
end

# Save the project
project.save

puts "Project updated successfully"
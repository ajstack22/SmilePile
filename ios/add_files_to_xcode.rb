#!/usr/bin/env ruby

require 'xcodeproj'

# Open the project
project_path = './SmilePile.xcodeproj'
project = Xcodeproj::Project.open(project_path)

# Get the main target
target = project.targets.first

# Get the Storage group
main_group = project.main_group
smilepile_group = main_group['SmilePile']
data_group = smilepile_group['Data']
storage_group = data_group['Storage']

# Files to add
files_to_add = [
  'SafeThumbnailGenerator.swift',
  'PhotoImportSession.swift',
  'PhotoImportCoordinator.swift'
]

# CoreData files
coredata_group = data_group['CoreData']
coredata_files = [
  'CoreDataMigrationManager.swift'
]

# Remove existing incorrect references first
storage_group.files.each do |file|
  if files_to_add.include?(file.name)
    file.remove_from_project
    puts "Removed incorrect reference for #{file.name}"
  end
end

coredata_group.files.each do |file|
  if coredata_files.include?(file.name)
    file.remove_from_project
    puts "Removed incorrect reference for #{file.name}"
  end
end

# Add Storage files with correct paths
files_to_add.each do |filename|
  # Just the filename, not the full path
  file_ref = storage_group.new_file(filename)

  # Add to build phases
  target.source_build_phase.add_file_reference(file_ref)
  puts "Added #{filename} to project with correct path"
end

# Add CoreData files with correct paths
coredata_files.each do |filename|
  # Just the filename, not the full path
  file_ref = coredata_group.new_file(filename)

  # Add to build phases
  target.source_build_phase.add_file_reference(file_ref)
  puts "Added #{filename} to project with correct path"
end

# Add test files
test_target = project.targets.find { |t| t.name == 'SmilePileTests' }
if test_target
  test_group = main_group['SmilePileTests']

  test_files = ['PhotoImportSafetyTests.swift']

  # Remove incorrect references
  test_group.files.each do |file|
    if test_files.include?(file.name)
      file.remove_from_project
      puts "Removed incorrect reference for #{file.name}"
    end
  end

  test_files.each do |filename|
    file_ref = test_group.new_file(filename)
    test_target.source_build_phase.add_file_reference(file_ref)
    puts "Added #{filename} to test target with correct path"
  end
end

# Save the project
project.save

puts "Project file updated successfully!"
#!/usr/bin/env python3

import re

def add_file_to_project():
    project_path = "/Users/adamstack/SmilePile/ios/SmilePile.xcodeproj/project.pbxproj"

    with open(project_path, 'r') as f:
        content = f.read()

    # Generate unique IDs
    file_ref_id = "2DAA" + "A" * 20
    build_file_id = "2DBB" + "B" * 20

    # Create file reference
    file_ref = f'\t\t{file_ref_id} /* SimplePhotoStorage.swift */ = {{isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = SimplePhotoStorage.swift; sourceTree = "<group>"; }};'

    # Add file reference after StorageManager.swift reference
    storage_ref_pattern = r'(3E624448C156BA4DB37DBB96 /\* StorageManager\.swift \*/ = \{[^}]+\};)'
    match = re.search(storage_ref_pattern, content)
    if match:
        content = content.replace(match.group(0), match.group(0) + '\n' + file_ref)
        print("Added file reference")

    # Create build file entry
    build_file = f'\t\t{build_file_id} /* SimplePhotoStorage.swift in Sources */ = {{isa = PBXBuildFile; fileRef = {file_ref_id} /* SimplePhotoStorage.swift */; }};'

    # Add build file after StorageManager build file
    build_pattern = r'(C3ECB12F78D3963C3875212A /\* StorageManager\.swift in Sources \*/ = \{[^}]+\};)'
    match = re.search(build_pattern, content)
    if match:
        content = content.replace(match.group(0), match.group(0) + '\n' + build_file)
        print("Added build file")

    # Add to Storage group children
    storage_group_pattern = r'(children = \([^)]*3E624448C156BA4DB37DBB96 /\* StorageManager\.swift \*/,)'
    match = re.search(storage_group_pattern, content)
    if match:
        new_children = match.group(0) + f'\n\t\t\t\t{file_ref_id} /* SimplePhotoStorage.swift */,'
        content = content.replace(match.group(0), new_children)
        print("Added to Storage group")

    # Add to Sources build phase
    sources_pattern = r'(C3ECB12F78D3963C3875212A /\* StorageManager\.swift in Sources \*/,)'
    match = re.search(sources_pattern, content)
    if match:
        new_sources = match.group(0) + f'\n\t\t\t\t{build_file_id} /* SimplePhotoStorage.swift in Sources */,'
        content = content.replace(match.group(0), new_sources)
        print("Added to Sources build phase")

    # Write back
    with open(project_path, 'w') as f:
        f.write(content)

    print("Successfully added SimplePhotoStorage.swift to project")

if __name__ == "__main__":
    add_file_to_project()
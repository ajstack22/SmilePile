#!/usr/bin/env python3

import sys
import re

def add_file_to_xcode_project(project_path, file_name):
    with open(project_path, 'r') as f:
        content = f.read()

    # Find the Services group ID
    services_group_match = re.search(r'([A-F0-9]+) /\* Services \*/ = \{', content)
    if not services_group_match:
        print("Could not find Services group")
        return False

    services_group_id = services_group_match.group(1)

    # Generate a new file reference ID (using a simple pattern)
    file_ref_id = "2D" + "A" * 22
    build_file_id = "2D" + "B" * 22

    # Add file reference
    file_ref = f'\t\t{file_ref_id} /* {file_name} */ = {{isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = {file_name}; sourceTree = "<group>"; }};'

    # Find the PBXFileReference section and add our file
    file_ref_section = re.search(r'/\* Begin PBXFileReference section \*/(.*?)/\* End PBXFileReference section \*/', content, re.DOTALL)
    if file_ref_section:
        new_section = file_ref_section.group(0).rstrip(' \t\n')
        new_section = new_section[:-len('/* End PBXFileReference section */')]
        new_section += file_ref + '\n/* End PBXFileReference section */'
        content = content.replace(file_ref_section.group(0), new_section)

    # Add to Services group
    services_group_pattern = f'{services_group_id} /\\* Services \\*/ = {{[^}}]+children = \\([^)]+\\);'
    services_match = re.search(services_group_pattern, content, re.DOTALL)
    if services_match:
        children_section = services_match.group(0)
        # Add our file reference to the children array
        new_children = children_section.replace(
            ');',
            f'\t\t\t\t{file_ref_id} /* {file_name} */,\n\t\t\t);'
        )
        content = content.replace(children_section, new_children)

    # Add to build phase
    build_phase_match = re.search(r'([A-F0-9]+) /\* Sources \*/ = \{[^}]+files = \([^)]+\);', content, re.DOTALL)
    if build_phase_match:
        build_phase = build_phase_match.group(0)
        # Create build file entry
        build_file = f'\t\t{build_file_id} /* {file_name} in Sources */ = {{isa = PBXBuildFile; fileRef = {file_ref_id} /* {file_name} */; }};'

        # Add to PBXBuildFile section
        build_file_section = re.search(r'/\* Begin PBXBuildFile section \*/(.*?)/\* End PBXBuildFile section \*/', content, re.DOTALL)
        if build_file_section:
            new_section = build_file_section.group(0).rstrip(' \t\n')
            new_section = new_section[:-len('/* End PBXBuildFile section */')]
            new_section += build_file + '\n/* End PBXBuildFile section */'
            content = content.replace(build_file_section.group(0), new_section)

        # Add to Sources build phase
        new_build_phase = build_phase.replace(
            ');',
            f'\t\t\t\t{build_file_id} /* {file_name} in Sources */,\n\t\t\t);'
        )
        content = content.replace(build_phase, new_build_phase)

    # Write back
    with open(project_path, 'w') as f:
        f.write(content)

    print(f"Added {file_name} to Xcode project")
    return True

if __name__ == "__main__":
    project_path = "/Users/adamstack/SmilePile/ios/SmilePile.xcodeproj/project.pbxproj"
    file_name = "SimplePhotoStorage.swift"
    add_file_to_xcode_project(project_path, file_name)
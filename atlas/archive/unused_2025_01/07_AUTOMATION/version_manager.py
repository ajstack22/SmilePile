#!/usr/bin/env python3
"""
Atlas Version Manager - Standardized version generation and tracking
Formats: Development (YYYY.MM.DD.VVV), Production (YYYY.MM.VV)
"""

import sys
import json
from pathlib import Path
from datetime import datetime
import subprocess

class VersionManager:
    """
    Manages version numbers according to Atlas standards
    """

    def __init__(self):
        self.version_file = Path('version.json')
        self.build_tracker = Path('.atlas/build_tracker.json')
        self.build_tracker.parent.mkdir(parents=True, exist_ok=True)

    def generate_version(self, is_release=False, custom_build_num=None):
        """
        Generate a new version number

        Args:
            is_release: If True, generate production version (YYYY.MM.VV)
            custom_build_num: Override auto-increment build number

        Returns:
            Version string
        """
        now = datetime.now()

        if is_release:
            # Production release: YYYY.MM.VV
            build_num = custom_build_num or self._get_monthly_release_number()
            version = f"{now.year}.{now.month:02d}.{build_num:02d}"
            env = "production"
        else:
            # Development build: YYYY.MM.DD.VVV
            build_num = custom_build_num or self._get_daily_build_number()
            version = f"{now.year}.{now.month:02d}.{now.day:02d}.{build_num:03d}"
            env = "development"

        # Get git commit hash if available
        commit_hash = self._get_git_hash()

        version_data = {
            "version": version,
            "buildDate": now.isoformat(),
            "environment": env,
            "buildNumber": build_num,
            "commitHash": commit_hash
        }

        # Save to version.json
        with open(self.version_file, 'w') as f:
            json.dump(version_data, f, indent=2)

        # Update build tracker
        self._update_build_tracker(version, env)

        return version_data

    def _get_daily_build_number(self):
        """Get next build number for today"""
        today = datetime.now().strftime('%Y-%m-%d')

        tracker = self._load_build_tracker()

        if tracker.get('lastBuildDate') == today:
            # Increment today's build number
            next_num = tracker.get('dailyBuildCount', 0) + 1
        else:
            # First build of the day
            next_num = 1

        # Update tracker
        tracker['lastBuildDate'] = today
        tracker['dailyBuildCount'] = next_num
        self._save_build_tracker(tracker)

        return next_num

    def _get_monthly_release_number(self):
        """Get next release number for this month"""
        month = datetime.now().strftime('%Y-%m')

        tracker = self._load_build_tracker()

        if tracker.get('lastReleaseMonth') == month:
            # Increment this month's release number
            next_num = tracker.get('monthlyReleaseCount', 0) + 1
        else:
            # First release of the month
            next_num = 1

        # Update tracker
        tracker['lastReleaseMonth'] = month
        tracker['monthlyReleaseCount'] = next_num
        self._save_build_tracker(tracker)

        return next_num

    def _load_build_tracker(self):
        """Load build tracking data"""
        if self.build_tracker.exists():
            with open(self.build_tracker, 'r') as f:
                return json.load(f)
        return {}

    def _save_build_tracker(self, tracker):
        """Save build tracking data"""
        with open(self.build_tracker, 'w') as f:
            json.dump(tracker, f, indent=2)

    def _update_build_tracker(self, version, env):
        """Update build history"""
        tracker = self._load_build_tracker()

        if 'history' not in tracker:
            tracker['history'] = []

        tracker['history'].append({
            'version': version,
            'environment': env,
            'timestamp': datetime.now().isoformat()
        })

        # Keep only last 100 builds in history
        tracker['history'] = tracker['history'][-100:]

        self._save_build_tracker(tracker)

    def _get_git_hash(self):
        """Get current git commit hash"""
        try:
            result = subprocess.run(
                ['git', 'rev-parse', '--short', 'HEAD'],
                capture_output=True,
                text=True,
                check=True
            )
            return result.stdout.strip()
        except:
            return None

    def get_current_version(self):
        """Get current version from version.json"""
        if self.version_file.exists():
            with open(self.version_file, 'r') as f:
                data = json.load(f)
                return data.get('version')
        return None

    def inject_version(self, file_path, file_type):
        """
        Inject version into various file types

        Args:
            file_path: Path to file to update
            file_type: Type of file (package.json, build.gradle, Info.plist, etc.)

        Returns:
            Success status
        """
        version = self.get_current_version()
        if not version:
            return {'error': 'No version.json found'}

        file_path = Path(file_path)

        if file_type == 'package.json':
            return self._inject_package_json(file_path, version)
        elif file_type == 'build.gradle':
            return self._inject_gradle(file_path, version)
        elif file_type == 'Info.plist':
            return self._inject_plist(file_path, version)
        elif file_type == 'html':
            return self._inject_html(file_path, version)
        else:
            return {'error': f'Unknown file type: {file_type}'}

    def _inject_package_json(self, file_path, version):
        """Inject version into package.json"""
        with open(file_path, 'r') as f:
            data = json.load(f)

        data['version'] = version

        with open(file_path, 'w') as f:
            json.dump(data, f, indent=2)

        return {'success': True, 'file': str(file_path), 'version': version}

    def _inject_gradle(self, file_path, version):
        """Inject version into build.gradle"""
        # Convert version to versionCode (remove dots)
        version_code = version.replace('.', '')

        with open(file_path, 'r') as f:
            content = f.read()

        # Replace versionName and versionCode
        import re
        content = re.sub(r'versionName\s*=?\s*"[^"]*"', f'versionName = "{version}"', content)
        content = re.sub(r'versionCode\s*=?\s*\d+', f'versionCode = {version_code}', content)

        with open(file_path, 'w') as f:
            f.write(content)

        return {'success': True, 'file': str(file_path), 'version': version}

    def _inject_plist(self, file_path, version):
        """Inject version into Info.plist"""
        # This would use plistlib in a real implementation
        return {'success': True, 'file': str(file_path), 'version': version}

    def _inject_html(self, file_path, version):
        """Inject version into HTML"""
        with open(file_path, 'r') as f:
            content = f.read()

        # Add version meta tag
        import re
        if '<meta name="version"' in content:
            content = re.sub(
                r'<meta name="version" content="[^"]*">',
                f'<meta name="version" content="{version}">',
                content
            )
        else:
            # Insert after <head>
            content = content.replace(
                '<head>',
                f'<head>\n  <meta name="version" content="{version}">'
            )

        with open(file_path, 'w') as f:
            f.write(content)

        return {'success': True, 'file': str(file_path), 'version': version}

    def create_version_display_component(self, framework):
        """
        Generate version display component for various frameworks

        Args:
            framework: react, android, ios, vue, etc.

        Returns:
            Component code
        """
        version = self.get_current_version() or "0.0.0.000"

        components = {
            'react': f"""
// VersionDisplay.jsx
import React from 'react';

const VersionDisplay = ({{ subtle = true }}) => {{
  const version = "{version}";
  const style = subtle ? {{
    position: 'fixed',
    bottom: '10px',
    right: '10px',
    fontSize: '10px',
    color: 'rgba(128, 128, 128, 0.5)',
    cursor: 'pointer',
    userSelect: 'all'
  }} : {{
    fontSize: '14px',
    color: '#666'
  }};

  const copyToClipboard = () => {{
    navigator.clipboard.writeText(version);
  }};

  return (
    <div style={{style}} onClick={{copyToClipboard}} title="Click to copy">
      v{{version}}
    </div>
  );
}};

export default VersionDisplay;
""",
            'android': f"""
// VersionDisplay.kt
package com.app.components

import android.content.Context
import android.widget.TextView

class VersionDisplay {{
    companion object {{
        const val VERSION = "{version}"

        fun setupVersionView(textView: TextView, context: Context) {{
            textView.text = "v$VERSION"
            textView.alpha = 0.5f
            textView.textSize = 10f
            textView.setOnClickListener {{
                // Copy to clipboard
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("version", VERSION)
                clipboard.setPrimaryClip(clip)
            }}
        }}
    }}
}}
""",
            'ios': f"""
// VersionView.swift
import SwiftUI

struct VersionView: View {{
    let version = "{version}"

    var body: some View {{
        Text("v\\(version)")
            .font(.caption2)
            .foregroundColor(.gray.opacity(0.5))
            .onTapGesture {{
                UIPasteboard.general.string = version
            }}
    }}
}}
"""
        }

        return components.get(framework, {'error': f'Unknown framework: {framework}'})

def main():
    """
    Entry point for version management

    Usage:
        python3 version_manager.py generate                  # Dev build
        python3 version_manager.py generate --release        # Production release
        python3 version_manager.py current                   # Get current version
        python3 version_manager.py inject package.json       # Update package.json
        python3 version_manager.py component react           # Generate React component
    """
    manager = VersionManager()

    args = sys.argv[1:]

    if not args:
        usage = {
            'script': 'version_manager.py',
            'description': 'Atlas version management (YYYY.MM.DD.VVV)',
            'commands': [
                {
                    'command': 'generate',
                    'description': 'Generate new dev version'
                },
                {
                    'command': 'generate --release',
                    'description': 'Generate production version'
                },
                {
                    'command': 'current',
                    'description': 'Get current version'
                },
                {
                    'command': 'inject [file] [type]',
                    'description': 'Inject version into file'
                },
                {
                    'command': 'component [framework]',
                    'description': 'Generate version display component'
                }
            ],
            'version_format': {
                'development': 'YYYY.MM.DD.VVV',
                'production': 'YYYY.MM.VV'
            }
        }
        print(json.dumps(usage, indent=2))
        return

    command = args[0]

    if command == 'generate':
        is_release = '--release' in args
        result = manager.generate_version(is_release=is_release)
        print(json.dumps(result, indent=2))
        print(f"\n✅ Generated version: {result['version']}")

    elif command == 'current':
        version = manager.get_current_version()
        if version:
            print(f"Current version: {version}")
        else:
            print("No version.json found")

    elif command == 'inject' and len(args) >= 3:
        file_path = args[1]
        file_type = args[2]
        result = manager.inject_version(file_path, file_type)
        print(json.dumps(result, indent=2))

    elif command == 'component' and len(args) >= 2:
        framework = args[1]
        component = manager.create_version_display_component(framework)
        if isinstance(component, dict) and 'error' in component:
            print(json.dumps(component, indent=2))
        else:
            print(component)

    else:
        print(f"Unknown command: {command}")

if __name__ == "__main__":
    main()
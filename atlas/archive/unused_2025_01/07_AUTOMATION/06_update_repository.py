#!/usr/bin/env python3
"""
Atlas Repository Update Script - Designed to be called by Claude
Manages documentation updates, README generation, and repository presentation
"""

import sys
import json
import os
from pathlib import Path
from datetime import datetime
import subprocess

class RepositoryUpdater:
    """
    Manages repository documentation and presentation
    """

    def __init__(self):
        self.docs_dir = Path('docs')
        self.update_dir = Path('.atlas/updates')
        self.update_dir.mkdir(parents=True, exist_ok=True)
        self.metadata_file = self.update_dir / 'metadata.json'
        self.metadata = self.load_metadata()

    def load_metadata(self):
        """Load repository metadata"""
        if self.metadata_file.exists():
            with open(self.metadata_file, 'r') as f:
                return json.load(f)
        return {
            'last_update': None,
            'documentation_sections': [],
            'readme_sections': [],
            'changelog_entries': [],
            'api_docs': {},
            'examples': []
        }

    def save_metadata(self):
        """Save repository metadata"""
        with open(self.metadata_file, 'w') as f:
            json.dump(self.metadata, f, indent=2)

    def analyze_repository(self):
        """
        Analyze repository structure and content

        Returns:
            Repository analysis results
        """
        analysis = {
            'structure': {},
            'documentation': {},
            'code_metrics': {},
            'missing_docs': [],
            'outdated_docs': []
        }

        # Analyze structure
        analysis['structure'] = {
            'has_readme': os.path.exists('README.md'),
            'has_license': os.path.exists('LICENSE'),
            'has_contributing': os.path.exists('CONTRIBUTING.md'),
            'has_changelog': os.path.exists('CHANGELOG.md'),
            'has_docs_dir': os.path.exists('docs'),
            'has_examples': os.path.exists('examples'),
            'has_tests': any(os.path.exists(d) for d in ['test', 'tests', '__tests__'])
        }

        # Check documentation completeness
        essential_docs = ['README.md', 'CONTRIBUTING.md', 'CHANGELOG.md']
        for doc in essential_docs:
            if not os.path.exists(doc):
                analysis['missing_docs'].append(doc)

        # Analyze code for documentation needs
        analysis['code_metrics'] = self._analyze_code_documentation()

        # Check for outdated documentation
        if os.path.exists('README.md'):
            readme_age = self._get_file_age('README.md')
            if readme_age > 30:  # Days
                analysis['outdated_docs'].append(f'README.md (last updated {readme_age} days ago)')

        return {
            'action': 'analyze_repository',
            'analysis': analysis,
            'recommendations': self._generate_recommendations(analysis)
        }

    def _analyze_code_documentation(self):
        """Analyze code documentation coverage"""
        metrics = {
            'total_files': 0,
            'documented_files': 0,
            'total_functions': 0,
            'documented_functions': 0
        }

        # Simple analysis for Python files
        for py_file in Path('.').rglob('*.py'):
            if '.atlas' in str(py_file):
                continue

            metrics['total_files'] += 1

            with open(py_file, 'r') as f:
                content = f.read()
                # Check for docstrings
                if '"""' in content or "'''" in content:
                    metrics['documented_files'] += 1

                # Count functions (simple heuristic)
                metrics['total_functions'] += content.count('def ')
                metrics['documented_functions'] += content.count('def ') if '"""' in content else 0

        if metrics['total_files'] > 0:
            metrics['documentation_coverage'] = round(
                (metrics['documented_files'] / metrics['total_files']) * 100, 2
            )
        else:
            metrics['documentation_coverage'] = 0

        return metrics

    def _get_file_age(self, filepath):
        """Get file age in days"""
        try:
            stat = os.stat(filepath)
            age = (datetime.now() - datetime.fromtimestamp(stat.st_mtime)).days
            return age
        except:
            return 0

    def _generate_recommendations(self, analysis):
        """Generate documentation recommendations"""
        recommendations = []

        if not analysis['structure']['has_readme']:
            recommendations.append('Create README.md with project overview')

        if not analysis['structure']['has_contributing']:
            recommendations.append('Add CONTRIBUTING.md with contribution guidelines')

        if not analysis['structure']['has_changelog']:
            recommendations.append('Create CHANGELOG.md to track version history')

        if analysis['code_metrics'].get('documentation_coverage', 0) < 70:
            recommendations.append('Increase code documentation coverage (currently {}%)'.format(
                analysis['code_metrics'].get('documentation_coverage', 0)
            ))

        return recommendations

    def update_readme(self, sections=None):
        """
        Update or create README.md

        Args:
            sections: Dictionary of sections to include

        Returns:
            README update results
        """
        if not sections:
            # Generate default sections
            sections = self._generate_default_readme_sections()

        content = []

        # Add header
        if 'title' in sections:
            content.append(f"# {sections['title']}\n")

        if 'description' in sections:
            content.append(f"{sections['description']}\n")

        # Add badges if provided
        if 'badges' in sections:
            content.append(' '.join(sections['badges']) + '\n')

        # Standard sections
        standard_sections = [
            ('features', '## Features'),
            ('installation', '## Installation'),
            ('usage', '## Usage'),
            ('api', '## API Documentation'),
            ('examples', '## Examples'),
            ('contributing', '## Contributing'),
            ('license', '## License')
        ]

        for key, header in standard_sections:
            if key in sections:
                content.append(f"\n{header}\n")
                content.append(f"{sections[key]}\n")

        # Write README
        readme_content = '\n'.join(content)
        with open('README.md', 'w') as f:
            f.write(readme_content)

        # Update metadata
        self.metadata['readme_sections'] = list(sections.keys())
        self.metadata['last_update'] = datetime.now().isoformat()
        self.save_metadata()

        return {
            'action': 'update_readme',
            'sections_updated': list(sections.keys()),
            'file': 'README.md',
            'size': len(readme_content)
        }

    def _generate_default_readme_sections(self):
        """Generate default README sections based on project"""
        sections = {
            'title': 'Project Name',
            'description': 'A brief description of what this project does.',
            'features': '- Feature 1\n- Feature 2\n- Feature 3',
            'installation': '```bash\nnpm install\n```',
            'usage': '```javascript\nconst app = require(\'app\');\n```',
            'contributing': 'Please read CONTRIBUTING.md for details.',
            'license': 'This project is licensed under the MIT License.'
        }

        # Try to detect actual project details
        if os.path.exists('package.json'):
            with open('package.json', 'r') as f:
                pkg = json.load(f)
                sections['title'] = pkg.get('name', 'Project Name')
                sections['description'] = pkg.get('description', sections['description'])

        return sections

    def update_changelog(self, version, changes, release_date=None):
        """
        Update CHANGELOG.md

        Args:
            version: Version number
            changes: List of changes or dict with categories
            release_date: Release date (defaults to today)

        Returns:
            Changelog update results
        """
        release_date = release_date or datetime.now().strftime('%Y-%m-%d')

        # Read existing changelog
        existing_content = ''
        if os.path.exists('CHANGELOG.md'):
            with open('CHANGELOG.md', 'r') as f:
                existing_content = f.read()
        else:
            existing_content = '# Changelog\n\nAll notable changes to this project will be documented in this file.\n\n'

        # Prepare new entry
        new_entry = [f"\n## [{version}] - {release_date}\n"]

        if isinstance(changes, dict):
            # Categorized changes
            for category, items in changes.items():
                new_entry.append(f"\n### {category}\n")
                for item in items:
                    new_entry.append(f"- {item}\n")
        else:
            # Simple list of changes
            for change in changes:
                new_entry.append(f"- {change}\n")

        # Insert new entry after header
        lines = existing_content.split('\n')
        insert_index = 0
        for i, line in enumerate(lines):
            if line.startswith('## ['):
                insert_index = i
                break
            elif i > 5:  # After header section
                insert_index = i
                break

        # Insert new entry
        new_content = lines[:insert_index] + [''.join(new_entry)] + lines[insert_index:]

        # Write updated changelog
        with open('CHANGELOG.md', 'w') as f:
            f.write('\n'.join(new_content))

        # Update metadata
        self.metadata['changelog_entries'].append({
            'version': version,
            'date': release_date,
            'changes': changes
        })
        self.save_metadata()

        return {
            'action': 'update_changelog',
            'version': version,
            'release_date': release_date,
            'changes_count': len(changes) if isinstance(changes, list) else sum(len(v) for v in changes.values())
        }

    def generate_api_docs(self, source_dir='.', output_dir='docs/api'):
        """
        Generate API documentation

        Args:
            source_dir: Source directory to scan
            output_dir: Output directory for docs

        Returns:
            API documentation generation results
        """
        Path(output_dir).mkdir(parents=True, exist_ok=True)

        api_docs = {}
        documented_items = 0

        # Scan Python files for API documentation
        for py_file in Path(source_dir).rglob('*.py'):
            if '.atlas' in str(py_file):
                continue

            module_docs = self._extract_python_api(py_file)
            if module_docs:
                api_docs[str(py_file)] = module_docs
                documented_items += len(module_docs.get('functions', []))
                documented_items += len(module_docs.get('classes', []))

        # Write API documentation
        for module_path, docs in api_docs.items():
            module_name = Path(module_path).stem
            doc_file = Path(output_dir) / f"{module_name}.md"

            content = [f"# {module_name} API\n"]

            if docs.get('module_doc'):
                content.append(f"{docs['module_doc']}\n")

            if docs.get('classes'):
                content.append("\n## Classes\n")
                for cls in docs['classes']:
                    content.append(f"\n### {cls['name']}\n")
                    if cls.get('docstring'):
                        content.append(f"{cls['docstring']}\n")

            if docs.get('functions'):
                content.append("\n## Functions\n")
                for func in docs['functions']:
                    content.append(f"\n### {func['name']}\n")
                    if func.get('signature'):
                        content.append(f"```python\n{func['signature']}\n```\n")
                    if func.get('docstring'):
                        content.append(f"{func['docstring']}\n")

            with open(doc_file, 'w') as f:
                f.write('\n'.join(content))

        # Update metadata
        self.metadata['api_docs'] = {
            'generated_at': datetime.now().isoformat(),
            'modules': list(api_docs.keys()),
            'documented_items': documented_items
        }
        self.save_metadata()

        return {
            'action': 'generate_api_docs',
            'output_dir': output_dir,
            'modules_documented': len(api_docs),
            'items_documented': documented_items
        }

    def _extract_python_api(self, file_path):
        """Extract API documentation from Python file"""
        try:
            with open(file_path, 'r') as f:
                content = f.read()

            docs = {
                'functions': [],
                'classes': []
            }

            # Simple extraction (for demonstration)
            lines = content.split('\n')
            for i, line in enumerate(lines):
                if line.strip().startswith('def '):
                    func_name = line.strip()[4:].split('(')[0]
                    # Look for docstring
                    docstring = None
                    if i + 1 < len(lines) and '"""' in lines[i + 1]:
                        docstring_lines = []
                        j = i + 1
                        while j < len(lines) and not (j > i + 1 and '"""' in lines[j]):
                            docstring_lines.append(lines[j].replace('"""', '').strip())
                            j += 1
                        docstring = ' '.join(docstring_lines).strip()

                    docs['functions'].append({
                        'name': func_name,
                        'signature': line.strip(),
                        'docstring': docstring
                    })

                elif line.strip().startswith('class '):
                    class_name = line.strip()[6:].split('(')[0].split(':')[0]
                    docs['classes'].append({
                        'name': class_name
                    })

            return docs if (docs['functions'] or docs['classes']) else None
        except:
            return None

    def create_example(self, name, code, description=None):
        """
        Create an example file

        Args:
            name: Example name
            code: Example code
            description: Example description

        Returns:
            Example creation results
        """
        examples_dir = Path('examples')
        examples_dir.mkdir(exist_ok=True)

        # Create example file
        example_file = examples_dir / f"{name}.py"

        content = []
        if description:
            content.append(f'"""\n{description}\n"""\n\n')
        content.append(code)

        with open(example_file, 'w') as f:
            f.write(''.join(content))

        # Update metadata
        self.metadata['examples'].append({
            'name': name,
            'file': str(example_file),
            'description': description,
            'created_at': datetime.now().isoformat()
        })
        self.save_metadata()

        return {
            'action': 'create_example',
            'name': name,
            'file': str(example_file)
        }

    def sync_documentation(self):
        """
        Synchronize all documentation

        Returns:
            Synchronization results
        """
        results = {
            'action': 'sync_documentation',
            'updated': [],
            'created': [],
            'errors': []
        }

        # Check and update README
        if not os.path.exists('README.md'):
            self.update_readme()
            results['created'].append('README.md')
        else:
            # Check if README needs update
            age = self._get_file_age('README.md')
            if age > 7:  # Update if older than 7 days
                self.update_readme()
                results['updated'].append('README.md')

        # Check CHANGELOG
        if not os.path.exists('CHANGELOG.md'):
            self.update_changelog('0.1.0', ['Initial release'])
            results['created'].append('CHANGELOG.md')

        # Generate API docs if needed
        if not os.path.exists('docs/api'):
            self.generate_api_docs()
            results['created'].append('docs/api')

        # Update metadata
        self.metadata['last_sync'] = datetime.now().isoformat()
        self.save_metadata()

        return results

def main():
    """
    Entry point when Claude runs this script

    Usage:
        python 06_update_repository.py analyze
        python 06_update_repository.py readme --title "Project" --description "Description"
        python 06_update_repository.py changelog 1.0.0 --changes "Feature 1|Bug fix 1"
        python 06_update_repository.py api-docs --source . --output docs/api
        python 06_update_repository.py example "basic_usage" --code "import app"
        python 06_update_repository.py sync
    """
    updater = RepositoryUpdater()

    # Parse command line arguments
    args = sys.argv[1:]

    if not args:
        # Return usage information
        usage = {
            'script': '06_update_repository.py',
            'description': 'Repository documentation and presentation manager',
            'commands': [
                {
                    'command': 'analyze',
                    'description': 'Analyze repository structure and documentation'
                },
                {
                    'command': 'readme --title "name" --description "desc"',
                    'description': 'Update README.md'
                },
                {
                    'command': 'changelog [version] --changes "change1|change2"',
                    'description': 'Update CHANGELOG.md'
                },
                {
                    'command': 'api-docs --source [dir] --output [dir]',
                    'description': 'Generate API documentation'
                },
                {
                    'command': 'example [name] --code "code" --description "desc"',
                    'description': 'Create example file'
                },
                {
                    'command': 'sync',
                    'description': 'Synchronize all documentation'
                }
            ]
        }
        print(json.dumps(usage, indent=2))
        return

    command = args[0]

    if command == 'analyze':
        result = updater.analyze_repository()
        print(json.dumps(result, indent=2))

    elif command == 'readme':
        sections = {}

        # Parse README sections
        i = 1
        while i < len(args):
            if args[i].startswith('--'):
                key = args[i][2:]
                if i + 1 < len(args):
                    sections[key] = args[i + 1]
                    i += 2
                else:
                    i += 1
            else:
                i += 1

        result = updater.update_readme(sections if sections else None)
        print(json.dumps(result, indent=2))

    elif command == 'changelog':
        version = args[1] if len(args) > 1 else '0.1.0'
        changes = []

        if '--changes' in args:
            idx = args.index('--changes')
            if idx + 1 < len(args):
                changes = args[idx + 1].split('|')

        result = updater.update_changelog(version, changes)
        print(json.dumps(result, indent=2))

    elif command == 'api-docs':
        source_dir = '.'
        output_dir = 'docs/api'

        if '--source' in args:
            idx = args.index('--source')
            if idx + 1 < len(args):
                source_dir = args[idx + 1]

        if '--output' in args:
            idx = args.index('--output')
            if idx + 1 < len(args):
                output_dir = args[idx + 1]

        result = updater.generate_api_docs(source_dir, output_dir)
        print(json.dumps(result, indent=2))

    elif command == 'example':
        if len(args) < 2:
            print(json.dumps({'error': 'Example name required'}, indent=2))
            return

        name = args[1]
        code = 'print("Example code")'
        description = None

        if '--code' in args:
            idx = args.index('--code')
            if idx + 1 < len(args):
                code = args[idx + 1]

        if '--description' in args:
            idx = args.index('--description')
            if idx + 1 < len(args):
                description = args[idx + 1]

        result = updater.create_example(name, code, description)
        print(json.dumps(result, indent=2))

    elif command == 'sync':
        result = updater.sync_documentation()
        print(json.dumps(result, indent=2))

    else:
        print(json.dumps({'error': f'Unknown command: {command}'}, indent=2))

if __name__ == "__main__":
    main()
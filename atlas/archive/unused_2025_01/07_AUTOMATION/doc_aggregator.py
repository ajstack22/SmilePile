#!/usr/bin/env python3
"""
Atlas Documentation Aggregator
Ensures documentation is created for every component and kept up-to-date
"""

import json
import os
from pathlib import Path
from datetime import datetime
from typing import Dict, List, Optional

class DocumentationAggregator:
    """
    Manages component documentation and ensures completeness
    """

    def __init__(self):
        self.atlas_dir = Path(__file__).parent.parent
        self.doc_dir = self.atlas_dir / 'component_docs'
        self.doc_dir.mkdir(exist_ok=True)
        self.interface_dir = self.atlas_dir / 'interfaces'
        self.interface_dir.mkdir(exist_ok=True)
        self.iteration_dir = self.atlas_dir / 'iterations'
        self.iteration_dir.mkdir(exist_ok=True)

    def create_component_doc(
        self,
        component_name: str,
        description: str,
        api: Dict = None,
        dependencies: List[str] = None,
        author: str = None
    ) -> Dict:
        """
        Create or update documentation for a component

        Args:
            component_name: Name of the component
            description: What the component does
            api: API/interface specification
            dependencies: Other components this depends on
            author: Agent or person creating the doc

        Returns:
            Result of documentation creation
        """
        doc_path = self.doc_dir / f'{component_name}.md'
        interface_path = self.interface_dir / f'{component_name}.json'

        # Create markdown documentation
        doc_content = self._generate_component_doc(
            component_name,
            description,
            api,
            dependencies,
            author
        )

        with open(doc_path, 'w') as f:
            f.write(doc_content)

        # Save interface specification
        if api:
            with open(interface_path, 'w') as f:
                json.dump(api, f, indent=2)

        return {
            'action': 'component_documented',
            'component': component_name,
            'doc_path': str(doc_path),
            'interface_path': str(interface_path) if api else None,
            'timestamp': datetime.now().isoformat()
        }

    def _generate_component_doc(
        self,
        name: str,
        description: str,
        api: Dict,
        dependencies: List[str],
        author: str
    ) -> str:
        """Generate markdown documentation for component"""
        doc = f"""# Component: {name}

**Created**: {datetime.now().strftime('%Y-%m-%d')}
**Author**: {author or 'Unknown'}

## Description

{description}

## Dependencies

"""
        if dependencies:
            for dep in dependencies:
                doc += f"- {dep}\n"
        else:
            doc += "No external dependencies\n"

        if api:
            doc += """
## API

### Methods
"""
            for method in api.get('methods', []):
                doc += f"""
#### `{method.get('name', 'unknown')}`

{method.get('description', '')}

**Parameters:**
"""
                for param in method.get('parameters', []):
                    doc += f"- `{param['name']}` ({param.get('type', 'any')}): {param.get('description', '')}\n"

                doc += f"""
**Returns:** {method.get('returns', 'void')}

**Example:**
```{method.get('language', 'javascript')}
{method.get('example', '// No example provided')}
```
"""

        doc += """
## Integration Points

This component integrates with the system through:

"""
        if api and api.get('integration_points'):
            for point in api['integration_points']:
                doc += f"- {point}\n"
        else:
            doc += "- Standard module import/export\n"

        doc += """
## Testing

### Test Coverage
- Target: 80%
- Current: TBD

### Test Files
"""
        if api and api.get('test_files'):
            for test_file in api['test_files']:
                doc += f"- {test_file}\n"
        else:
            doc += "- Tests to be added\n"

        doc += """
## Known Issues

None reported.

## Future Improvements

- Performance optimization
- Additional error handling
- Extended API surface
"""

        return doc

    def create_iteration_doc(
        self,
        iteration: int,
        feature_name: str,
        what_was_added: str,
        how_it_works: str,
        tests_added: List[str] = None,
        known_limitations: List[str] = None
    ) -> Dict:
        """
        Document an iteration

        Args:
            iteration: Iteration number
            feature_name: Name of the feature added
            what_was_added: Description of changes
            how_it_works: Technical explanation
            tests_added: List of tests created
            known_limitations: Current limitations

        Returns:
            Result of documentation creation
        """
        doc_path = self.iteration_dir / f'iteration_{iteration:03d}.md'

        doc_content = f"""# Iteration {iteration}: {feature_name}

**Date**: {datetime.now().strftime('%Y-%m-%d')}

## What Was Added

{what_was_added}

## How It Works

{how_it_works}

## Tests Added

"""
        if tests_added:
            for test in tests_added:
                doc_content += f"- {test}\n"
        else:
            doc_content += "- No tests added (NEEDS ATTENTION)\n"

        doc_content += """
## API Changes

Any new or modified APIs in this iteration:

```
// Document API changes here
```

## Dependencies Added

- None

## Known Limitations

"""
        if known_limitations:
            for limitation in known_limitations:
                doc_content += f"- {limitation}\n"
        else:
            doc_content += "- None identified\n"

        doc_content += """
## Next Steps

- Continue to next iteration
- Address any limitations
- Increase test coverage
"""

        with open(doc_path, 'w') as f:
            f.write(doc_content)

        return {
            'action': 'iteration_documented',
            'iteration': iteration,
            'feature': feature_name,
            'doc_path': str(doc_path),
            'timestamp': datetime.now().isoformat()
        }

    def aggregate_dependencies(self, component: str) -> Dict:
        """
        Aggregate all documentation for a component's dependencies

        Args:
            component: Component name

        Returns:
            Aggregated documentation
        """
        # Read component's doc to find dependencies
        doc_path = self.doc_dir / f'{component}.md'
        interface_path = self.interface_dir / f'{component}.json'

        aggregated = {
            'component': component,
            'documentation': None,
            'dependencies': {}
        }

        # Read main component doc
        if doc_path.exists():
            with open(doc_path) as f:
                aggregated['documentation'] = f.read()

        # Read interface if exists
        if interface_path.exists():
            with open(interface_path) as f:
                interface = json.load(f)
                deps = interface.get('dependencies', [])

                # Read each dependency's documentation
                for dep in deps:
                    dep_doc_path = self.doc_dir / f'{dep}.md'
                    if dep_doc_path.exists():
                        with open(dep_doc_path) as f:
                            aggregated['dependencies'][dep] = {
                                'documentation': f.read()
                            }

                    dep_interface_path = self.interface_dir / f'{dep}.json'
                    if dep_interface_path.exists():
                        with open(dep_interface_path) as f:
                            aggregated['dependencies'][dep]['interface'] = json.load(f)

        return aggregated

    def check_documentation_completeness(self) -> Dict:
        """
        Check if all components have documentation

        Returns:
            Report on documentation completeness
        """
        # Find all source files
        source_extensions = ['.py', '.js', '.ts', '.java', '.kt', '.swift', '.go', '.rs']
        source_files = []

        for ext in source_extensions:
            source_files.extend(self.atlas_dir.parent.rglob(f'*{ext}'))

        # Extract component names (simplified - just use file names)
        components = set()
        for file in source_files:
            if not any(skip in str(file) for skip in ['node_modules', '.git', 'build', 'dist']):
                components.add(file.stem)

        # Check which have documentation
        documented = set()
        for doc_file in self.doc_dir.glob('*.md'):
            documented.add(doc_file.stem)

        missing = components - documented
        coverage = len(documented) / len(components) * 100 if components else 0

        return {
            'total_components': len(components),
            'documented': len(documented),
            'missing': len(missing),
            'coverage_percentage': round(coverage, 2),
            'missing_components': sorted(list(missing))[:20],  # First 20
            'documented_components': sorted(list(documented))
        }

    def generate_architecture_doc(self) -> Dict:
        """
        Generate overall architecture documentation from components

        Returns:
            Result of architecture doc generation
        """
        arch_doc = """# System Architecture

**Generated**: """ + datetime.now().strftime('%Y-%m-%d') + """

## Overview

This document describes the overall architecture of the system based on documented components.

## Components

"""
        # List all documented components
        for doc_file in sorted(self.doc_dir.glob('*.md')):
            component = doc_file.stem
            arch_doc += f"### {component}\n\n"

            # Read first few lines of description
            with open(doc_file) as f:
                lines = f.readlines()
                for line in lines:
                    if line.startswith('## Description'):
                        # Read until next section
                        idx = lines.index(line)
                        for desc_line in lines[idx+2:idx+5]:
                            if not desc_line.startswith('#'):
                                arch_doc += desc_line
                        break

        arch_doc += """
## Component Relationships

```mermaid
graph TD
"""
        # Generate component relationship diagram
        relationships = self._analyze_component_relationships()
        for rel in relationships:
            arch_doc += f"    {rel['from']} --> {rel['to']}\n"

        arch_doc += """```

## Data Flow

1. User input enters through UI components
2. Business logic processes in service layer
3. Data persists through repository layer
4. Results return to UI

## Key Design Decisions

- Modular architecture for maintainability
- Clear separation of concerns
- Test-driven development approach
- Iterative enhancement model

## Testing Strategy

- Unit tests for each component
- Integration tests for workflows
- End-to-end tests for critical paths
- Performance tests for bottlenecks
"""

        arch_path = self.atlas_dir / 'ARCHITECTURE.md'
        with open(arch_path, 'w') as f:
            f.write(arch_doc)

        return {
            'action': 'architecture_generated',
            'path': str(arch_path),
            'components_included': len(list(self.doc_dir.glob('*.md'))),
            'timestamp': datetime.now().isoformat()
        }

    def _analyze_component_relationships(self) -> List[Dict]:
        """Analyze relationships between components"""
        relationships = []

        for interface_file in self.interface_dir.glob('*.json'):
            with open(interface_file) as f:
                interface = json.load(f)
                component = interface_file.stem

                for dep in interface.get('dependencies', []):
                    relationships.append({
                        'from': component,
                        'to': dep
                    })

        return relationships

    def create_api_documentation(self) -> Dict:
        """
        Generate API documentation from all interfaces

        Returns:
            Result of API doc generation
        """
        api_doc = """# API Documentation

**Generated**: """ + datetime.now().strftime('%Y-%m-%d') + """

## Overview

Complete API reference for all system components.

"""

        for interface_file in sorted(self.interface_dir.glob('*.json')):
            with open(interface_file) as f:
                interface = json.load(f)
                component = interface_file.stem

                api_doc += f"## {component}\n\n"

                for method in interface.get('methods', []):
                    api_doc += f"### `{method.get('name', 'unknown')}`\n\n"
                    api_doc += f"{method.get('description', '')}\n\n"
                    api_doc += "**Signature:**\n```\n"
                    api_doc += f"{method.get('signature', 'No signature provided')}\n"
                    api_doc += "```\n\n"

        api_path = self.atlas_dir / 'API.md'
        with open(api_path, 'w') as f:
            f.write(api_doc)

        return {
            'action': 'api_documentation_generated',
            'path': str(api_path),
            'components': len(list(self.interface_dir.glob('*.json'))),
            'timestamp': datetime.now().isoformat()
        }

def main():
    """CLI interface for documentation aggregator"""
    import sys

    aggregator = DocumentationAggregator()
    args = sys.argv[1:]

    if not args:
        print(json.dumps({
            'error': 'No command provided',
            'usage': {
                'component': 'doc_aggregator.py component [name] [description] --api api.json --deps dep1,dep2',
                'iteration': 'doc_aggregator.py iteration [num] [feature] [what] [how]',
                'check': 'doc_aggregator.py check',
                'aggregate': 'doc_aggregator.py aggregate [component]',
                'architecture': 'doc_aggregator.py architecture',
                'api': 'doc_aggregator.py api'
            }
        }, indent=2))
        return

    command = args[0]

    if command == 'component' and len(args) >= 3:
        name = args[1]
        description = args[2]

        api = None
        dependencies = None

        # Parse optional args
        if '--api' in args:
            idx = args.index('--api')
            if idx + 1 < len(args):
                api_file = args[idx + 1]
                if Path(api_file).exists():
                    with open(api_file) as f:
                        api = json.load(f)

        if '--deps' in args:
            idx = args.index('--deps')
            if idx + 1 < len(args):
                dependencies = args[idx + 1].split(',')

        result = aggregator.create_component_doc(name, description, api, dependencies)
        print(json.dumps(result, indent=2))

    elif command == 'iteration' and len(args) >= 5:
        iteration = int(args[1])
        feature = args[2]
        what = args[3]
        how = args[4]

        tests = None
        limitations = None

        if '--tests' in args:
            idx = args.index('--tests')
            if idx + 1 < len(args):
                tests = args[idx + 1].split(',')

        if '--limitations' in args:
            idx = args.index('--limitations')
            if idx + 1 < len(args):
                limitations = args[idx + 1].split(',')

        result = aggregator.create_iteration_doc(
            iteration, feature, what, how, tests, limitations
        )
        print(json.dumps(result, indent=2))

    elif command == 'check':
        result = aggregator.check_documentation_completeness()
        print(json.dumps(result, indent=2))

    elif command == 'aggregate' and len(args) >= 2:
        component = args[1]
        result = aggregator.aggregate_dependencies(component)
        print(json.dumps(result, indent=2, default=str))

    elif command == 'architecture':
        result = aggregator.generate_architecture_doc()
        print(json.dumps(result, indent=2))

    elif command == 'api':
        result = aggregator.create_api_documentation()
        print(json.dumps(result, indent=2))

    else:
        print(json.dumps({
            'error': f'Unknown command: {command}'
        }, indent=2))

if __name__ == '__main__':
    main()
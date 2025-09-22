"""
Atlas Automation Module - Auto-initializes context injection for Claude Code
"""

import os
import sys
import logging
from pathlib import Path

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

def auto_initialize_context_injection():
    """
    Automatically initialize context injection when this module is imported
    This enables zero-configuration usage in Claude Code
    """
    try:
        # Check if we should auto-initialize
        if os.getenv('ATLAS_DISABLE_AUTO_CONTEXT') == 'true':
            logger.info("Atlas auto-context injection disabled by environment variable")
            return False

        # Import and register the integration
        try:
            from .claude_code_integration import register, get_integration
        except ImportError:
            # Handle when run directly
            import sys
            sys.path.insert(0, str(Path(__file__).parent))
            from claude_code_integration import register, get_integration

        # Attempt registration
        if register():
            logger.info("✅ Atlas context injection auto-initialized successfully")

            # Run validation in background
            integration = get_integration()
            if integration and integration.context_integration:
                validation = integration.context_integration.validate_integration()
                if validation['status'] != 'healthy':
                    logger.warning(f"Context injection validation warnings: {validation}")

            return True
        else:
            logger.warning("Atlas context injection could not be auto-initialized")
            return False

    except Exception as e:
        logger.debug(f"Atlas context injection not initialized: {e}")
        return False

# Auto-initialize when module is imported
_initialized = auto_initialize_context_injection()

# Export key functions for manual control
try:
    from .claude_code_integration import (
        get_integration,
        register,
        task_hook
    )

    from .task_context_integration import (
        TaskContextIntegration,
        initialize_integration
    )

    from .enhanced_context_injector import (
        EnhancedContextInjector
    )
except ImportError:
    # Handle absolute imports when run as script
    import sys
    from pathlib import Path
    sys.path.insert(0, str(Path(__file__).parent))

    from claude_code_integration import (
        get_integration,
        register,
        task_hook
    )

    from task_context_integration import (
        TaskContextIntegration,
        initialize_integration
    )

    from enhanced_context_injector import (
        EnhancedContextInjector
    )

__all__ = [
    'get_integration',
    'register',
    'task_hook',
    'TaskContextIntegration',
    'initialize_integration',
    'EnhancedContextInjector',
    '_initialized'
]

__version__ = '1.0.0'
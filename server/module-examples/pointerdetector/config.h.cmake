#ifndef __KURENTO_MODULE_POINTERDETECTOR_CONFIG_H__
#define __KURENTO_MODULE_POINTERDETECTOR_CONFIG_H__

/* Version */
#cmakedefine VERSION "@VERSION@"

/* Package name */
#cmakedefine PACKAGE "@PACKAGE@"

/* The gettext domain name */
#cmakedefine GETTEXT_PACKAGE "@GETTEXT_PACKAGE@"

/* Tests will generate files for manual check if this macro is defined */
#cmakedefine MANUAL_CHECK

/* Root URI with test files (e.g. http:// or file://) */
#cmakedefine TEST_FILES_LOCATION "@TEST_FILES_LOCATION@"

/* Library installation directory */
#cmakedefine KURENTO_MODULES_SO_DIR "@KURENTO_MODULES_SO_DIR@"

#endif /* __KURENTO_MODULE_POINTERDETECTOR_CONFIG_H__ */

#ifndef __KMS_ELEMENTS_CONFIG_H__
#define __KMS_ELEMENTS_CONFIG_H__

/* Version */
#cmakedefine VERSION "@VERSION@"

/* Package name */
#cmakedefine PACKAGE "@PACKAGE@"

/* The gettext domain name */
#cmakedefine GETTEXT_PACKAGE "@GETTEXT_PACKAGE@"

/* Tests will generate files for manual check if this macro is defined */
#cmakedefine MANUAL_CHECK

/* Binary files directory */
#cmakedefine BINARY_LOCATION "@BINARY_LOCATION@"

/* Library installation directory */
#cmakedefine KURENTO_MODULES_SO_DIR "@KURENTO_MODULES_SO_DIR@"

#endif /* __KMS_ELEMENTS_CONFIG_H__ */

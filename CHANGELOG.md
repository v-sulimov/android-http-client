# Changelog

All notable changes to this project will be documented in this file.

## [1.0.0] - 01/07/2025

### Added

- Initial release of Http Client library
- Core features:
    - Supported HTTP Methods: GET, POST, PUT, DELETE, HEAD, OPTIONS, and PATCH.
    - Custom SSL Support: Add custom X.509 certificates for secure connections.
    - Request Interceptors: Modify requests (e.g., add headers) before they are sent.
    - Configurable Timeouts: Set read and connect timeouts for requests.
    - Redirect Handling: Optionally follow HTTP redirects.
    - Type-Safe API: Dedicated request classes for each HTTP method.
    - Error Handling: Comprehensive exception handling for network and HTTP errors.
- Documentation:
    - README with usage instructions
    - VERSIONING policy
    - CHANGELOG template

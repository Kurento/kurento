# Security Policy

## Supported versions

Security fixes are released only for the latest minor release of Kurento 7.x. Users of older versions should upgrade to receive them.

| Version | Supported |
| ------- | --------- |
| 7.3.x   | Yes       |
| < 7.3   | No        |

## Reporting a vulnerability

**Please do not report security vulnerabilities through public GitHub issues, pull requests, discussions or the mailing list.**

Report them privately through GitHub's private vulnerability reporting:

1. Go to the [Security tab](https://github.com/Kurento/kurento/security) of this repository.
2. Click **Report a vulnerability**.
3. Fill in the form.

Please include as much of the following as you can:

- Affected component (e.g. Kurento Media Server, a specific endpoint or module, Java or JavaScript client).
- Affected version(s) and how Kurento was installed (Docker image, Debian packages, built from source).
- Description of the issue and its impact.
- Steps to reproduce, proof-of-concept code or a test case.
- A suggested fix or patch, if you have one.

## What to expect

- We will acknowledge your report within 7 days.
- We will confirm the issue, assess its severity and keep you informed of our progress through the private advisory.
- Fixes are developed privately and released in a new patch version. After that, the advisory is published and a CVE is requested when appropriate.
- Reporters are credited in the advisory unless they prefer to remain anonymous.

We ask that you give us a reasonable amount of time to release a fix before you disclose the issue publicly. If you have a disclosure deadline, please tell us in your report.

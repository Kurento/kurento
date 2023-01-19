---
name: Bug report
about: Create a report to help us improve. Note that reports that are not bugs will generally not be accepted.
title: ''
labels: ''
assignees: ''
---

## Prerequisites

These are MANDATORY, otherwise the issue will be automatically closed.

<!-- Fill with an 'x'. -->
* [] I agree to fill this issue template.
* [] I have read the [Troubleshooting Guide] and [Support Instructions].

[Troubleshooting Guide]: https://doc-kurento.readthedocs.io/en/latest/user/troubleshooting.html
[Support Instructions]: https://github.com/Kurento/kurento/blob/main/.github/SUPPORT.md


## Issue description

<!--
A clear and concise description of what the bug is.

Debug logs or source code snippets should go inside ```triple backquotes```.
-->


## Context

<!--
How has this issue affected you? What are you trying to accomplish?
Providing context helps us come up with a solution.
-->


## How to reproduce?

<!--
Explain the exact steps that other developer should follow in order to
reproduce the same issue.

For example:
1. Create this pipeline: "..."
2. Use these settings: "..."
3. Click "Start"
4. See error
-->


## Expected & current behavior

<!-- Tell us what should happen, and what happens instead. -->


## (Optional) Possible solution

<!--
Not obligatory, but suggest a fix/reason for the bug,
or ideas on how to implement the solution.
-->


## Info about your environment

<!--
Include as many relevant details about the environment where you experienced
the issue. Include things like:

* What Kurento Endpoints are used, and how they are connected.
* If you are configuring STUN or TURN in Kurento and/or in the browsers.
* If the WebRTC streams are being relayed through your TURN servers.
* The network topology between servers / services / containers / etc.
* If there are any web proxies.

ANYTHING that you think might be relevant or useful.
-->


### About Kurento Media Server

* Kurento version:  <!-- E.g. 7.0.0, nightly -->
* Server OS:          <!-- E.g. Ubuntu 20.04 (Focal) -->
* Installation method:
    <!-- Fill with an 'x' in the boxes that apply. -->
  - [] [apt-get]
  - [] [Docker]
  - [] [AWS CloudFormation]
  - [] [Built from sources]

[apt-get]: https://doc-kurento.readthedocs.io/en/latest/user/installation.html#installation-local
[Docker]: https://doc-kurento.readthedocs.io/en/latest/user/installation.html#installation-docker
[AWS CloudFormation]: https://doc-kurento.readthedocs.io/en/latest/user/installation.html#installation-aws
[Built from sources]: https://doc-kurento.readthedocs.io/en/latest/dev/dev_guide.html#dev-sources


### About your Application Server

* Programming Language:  <!-- E.g. Java, Node.js, browser JavaScript, etc. -->
* Kurento Client version:     <!-- E.g. 7.0.0, nightly -->


### About end-user clients

* Device(s):    <!-- E.g. PC, Mac, Android, iPhone, etc. -->
* OS(es):        <!-- E.g. Ubuntu 20.04, Windows 10, iOS 12, etc. -->
* Browser(s):  <!-- E.g. Firefox 74, Chrome 80, Safari 12.0, etc. -->


### Run these commands

<!--
Run these commands in your Kurento machine, and paste the output
inside the ```triple backquotes``` to preserve formatting.
-->

```
cat /etc/lsb-release
```

```
kurento-media-server --version
```

```
dpkg -l | grep -Pi 'kurento|kms-|gst.*1.5|nice'
```

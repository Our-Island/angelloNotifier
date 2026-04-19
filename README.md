<div align="center">

<img height="256" alt="angellonotifier-icon" src="https://raw.githubusercontent.com/Our-Island/angelloNotifier/refs/heads/master/angellonotifier-icon.png" />

angelloNotifier
---

A velocity announcement and notification sending and managing plugin.

[![Visitors](https://api.visitorbadge.io/api/visitors?path=https%3A%2F%2Fgithub.com%2FOur-Island%2FangelloNotifier&labelColor=%23444444&countColor=%23f24822&style=flat-square&labelStyle=none)](https://visitorbadge.io/status?path=https://github.com/Our-Island/angelloNotifier/)
[![Stars](https://img.shields.io/github/stars/Our-Island/angelloNotifier?style=flat-square&logo=data:image/svg%2bxml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZlcnNpb249IjEiIHdpZHRoPSIxNiIgaGVpZ2h0PSIxNiI+PHBhdGggZD0iTTggLjI1YS43NS43NSAwIDAgMSAuNjczLjQxOGwxLjg4MiAzLjgxNSA0LjIxLjYxMmEuNzUuNzUgMCAwIDEgLjQxNiAxLjI3OWwtMy4wNDYgMi45Ny43MTkgNC4xOTJhLjc1MS43NTEgMCAwIDEtMS4wODguNzkxTDggMTIuMzQ3bC0zLjc2NiAxLjk4YS43NS43NSAwIDAgMS0xLjA4OC0uNzlsLjcyLTQuMTk0TC44MTggNi4zNzRhLjc1Ljc1IDAgMCAxIC40MTYtMS4yOGw0LjIxLS42MTFMNy4zMjcuNjY4QS43NS43NSAwIDAgMSA4IC4yNVoiIGZpbGw9IiNlYWM1NGYiLz48L3N2Zz4=&logoSize=auto&label=Stars&labelColor=444444&color=eac54f)](https://github.com/Our-Island/angelloNotifier/)
[![GitHub CI](https://img.shields.io/github/actions/workflow/status/Our-Island/angelloNotifier/ci.yml?style=flat-square&labelColor=444444&branch=master&label=GitHub%20CI&logo=github)](https://github.com/Our-Island/angelloNotifier/actions/workflows/ci.yml)
[![Hangar](https://img.shields.io/badge/Hangar-angelloNotifier-004ee9?style=flat-square&labelColor=444444)](https://hangar.papermc.io/Our-Island/angelloNotifier)
[![Modrinth](https://img.shields.io/badge/Modrinth-angelloNotifier-22ff84?style=flat-square&labelColor=444444)](https://modrinth.com/plugin/angellonotifier/)

</div>

## Introduction

angelloNotifier is a proxy-side notice board for Velocity. It lets you publish one network-wide notice on the proxy,
store it in YAML, and let players reopen that same notice later from a shared history.

The plugin is designed as a public notice board rather than a private mailbox. Notices are global, while each player
only has their own read state. Join-time previews, ignored servers, MiniMessage support, and per-notice lifetime
settings are all built around that model.

## Installation

Build the jar or download a release, place it in the Velocity `plugins/` directory, and start the proxy once. The plugin
will create its working directory and write default resource files on first startup. After that, adjust
`plugins/angelloNotifier/config.yml` to fit your network and either restart the proxy or run `/angello reload`.

If you are updating an existing installation, it is still a good idea to keep a backup of both `config.yml` and
`data.yml` before replacing the jar. The data format is simple and human-readable, but a backup makes rollback much
easier if you are testing new behaviour.

## Quick start

The base command is `/angello`, with `/angellonotifier`, `/anno`, and `/noticeboard` available as aliases. For players,
the usual workflow is very small. `/angello inbox` opens the paged notice list in chat, `/angello show <id>` reprints a
specific notice, and `/angello unread` reports how many notices are still unread for that player.

For administrators, the central command is:

```text
/angello send <title> || <body> [|| lifetime]
```

The title is the first segment. The body is the second segment. The optional third segment is the lifetime for that one
notice only. If the lifetime is omitted, the plugin falls back to `settings.default-notification-lifetime` from
`config.yml`.

A simple example looks like this:

```text
/angello send <gold>Maintenance</gold> || <red>The network will restart in 10 minutes.</red>
```

A notice with a custom lifetime looks like this:

```text
/angello send <yellow>Reminder</yellow> || Please read the event rules before joining. || 3
```

If you want multiple body lines, either type actual new lines in the command input or use `\n` inside the body segment.
The plugin stores the body as a list of lines and prints it back as a block, with the notice board prefix on its own
first line for multi-line output.

Administrative review commands are `/angello list [page]`, `/angello delete <id>`, and `/angello reload`. Notice IDs do
not change after deletion. If notice `#12` is deleted, that ID remains gone, while later notices continue with higher
IDs.

## Documentation

The GitHub Wiki contains the detailed reference pages for the current implementation. The wiki home page explains the
overall model and intended behaviour. The configuration page documents every setting in `config.yml`. The data page
describes the exact structure of `data.yml`, including player state. The command page explains syntax, aliases,
permissions, and examples in more detail.

You can start here:

[Home](https://github.com/Our-Island/angelloNotifier/wiki)  
[config.yml](https://github.com/Our-Island/angelloNotifier/wiki/config.yml)  
[data.yml](https://github.com/Our-Island/angelloNotifier/wiki/data.yml)  
[Commands](https://github.com/Our-Island/angelloNotifier/wiki/Commands)

## Feedback

Please use GitHub Issues for bug reports and feature requests. When reporting a bug, include your Velocity version, the
angelloNotifier version, your configuration files, and the relevant console logs so the issue can be reproduced.

If the problem involves command parsing or formatting, include the exact command you entered and the exact output you
received. If the problem involves notice state, include the affected part of `data.yml` as well. That usually makes the
difference between a report that can be investigated quickly and one that needs several rounds of follow-up questions.

## Contributing

Contributions are welcome. Fork the repository, create a feature branch, and keep changes focused and easy to review.
When opening a Pull Request, explain what changed, why it changed, and how to test it.

If your changes affect configuration structure, command behaviour, notice persistence, or i18n, please update the
README, the wiki pages, and the shipped resource files where appropriate. Documentation drift is easy to introduce in a
plugin like this because command syntax and stored data shape are part of the public surface of the project.

## License

This project is licensed under the MIT License. See
the [LICENSE](https://github.com/Our-Island/angelloNotifier/blob/master/LICENSE) file for details.

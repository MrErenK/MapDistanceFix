# MapDistanceFix

A very simple Minecraft Fabric mod that fixes map player indicators to always show the player's position and direction, even when outside map boundaries, making it much easier to navigate treasure maps and other map items.

---

## Why This Mod?

In vanilla Minecraft, when you're far from a map's boundaries, the player indicator either disappears entirely or shows as a generic "player_off_map" and "player_off_limits" decorations without directional information. This mod fixes that by:

1. Converting off-map player decorations to regular player decorations
2. Maintaining correct rotation to show which direction you're facing
3. Ensuring the player is always visible for better navigation.
4. Adding distance calculation to map center or to the nearest structure on the map

---

## Installation

### Requirements

- **Minecraft**: 1.20.1
- **Fabric Loader**: 0.18.4
- **Fabric API**: 0.92.7+1.20.1

### Steps

1. Install [Fabric Loader](https://fabricmc.net/use/)
2. Download [Fabric API](https://modrinth.com/mod/fabric-api)
3. Download the latest release of MapDistanceFix
4. Place both `.jar` files in your `mods` folder
5. Launch Minecraft with the Fabric profile

---

## Compatibility

- **Server Compatibility**: Does not work on vanilla servers. But if the mod is installed on the server, its not required to install on the client.
- **Other Mods**: Should be compatible with most other mods that don't heavily modify maps.

---

## Troubleshooting

### Common Issues

**Player decoration not showing**
- The mod needs to see at least one regular player decoration first to cache the decoration type

### Getting Help

If you encounter issues:
1. Check the latest logs for error messages
2. Verify all dependencies are correctly installed
3. Try with minimal mods to isolate conflicts
4. Report issues on the [GitHub Issues page](https://github.com/mrerenk/MapDistanceFix/issues)

---

## Building from Source

### Development Setup

1. Clone the repository: `git clone https://github.com/mrerenk/MapDistanceFix.git`
2. Open in your favorite IDE (IntelliJ IDEA recommended)
3. Run `./gradlew genEclipseRuns` or `./gradlew genIntellijRuns`
4. Use the generated run configurations for testing

### Building

```bash
./gradlew build
```

The built mod will be located in `build/libs/`

---

## Contributing

Contributions are welcome! Here's how you can help:

### Pull Requests

- Follow existing code style and conventions
- Add appropriate comments and documentation
- Test thoroughly before submitting
- Update README if adding new features

### Bug Reports

- Use the GitHub Issues page
- Include Minecraft version, mod version, and log files
- Describe steps to reproduce the issue

---

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE) for details.

---

## Acknowledgments

- **mappings.dev** for the huge help with finding proper functions
- **Fabric Team** for the excellent modding framework
- **Minecraft Community** for inspiration and feedback
- **Contributors** who help improve this mod

<br>

**Made with ❤️ by MrErenK for the Minecraft community**

*For questions, suggestions, or support, feel free to open an issue on GitHub!*

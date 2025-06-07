# MapDistanceFix

A very simple Minecraft Fabric mod that fixes map player indicators to always show the player's position and direction, even when outside map boundaries, making it much easier to navigate treasure maps and other map items.

## ✨ Features

- **Always Visible Player**: Ensures the player icon is always visible on maps, even when far outside the map boundaries
- **Correct Direction**: Shows the player's actual facing direction instead of a generic off-map indicator
- **Edge Positioning**: Intelligently positions the player icon at the appropriate map edge when outside boundaries
- **Performance Optimized**: Minimal impact on game performance with smart caching and efficient calculations
- **Client-Side Only**: No server modifications required - works on any server

## 🎯 Why This Mod?

In vanilla Minecraft, when you're far from a map's boundaries, the player indicator either disappears entirely or shows as a generic "player_off_map" decoration without directional information. This mod fixes that by:

1. Converting off-map player decorations to regular player decorations
2. Positioning the player icon at the appropriate map edge
3. Maintaining correct rotation to show which direction you're facing
4. Ensuring the player is always visible for better navigation

## 📦 Installation

### Requirements
- **Minecraft**: 1.21.5
- **Fabric Loader**: 0.16.14 or later
- **Fabric API**: 0.126.0+1.21.5 or later

### Steps
1. Install [Fabric Loader](https://fabricmc.net/use/)
2. Download [Fabric API](https://modrinth.com/mod/fabric-api)
3. Download the latest release of MapDistanceFix
4. Place both `.jar` files in your `mods` folder
5. Launch Minecraft with the Fabric profile

## 🔧 Technical Details

### How It Works

The mod uses Fabric's Mixin system to modify map decoration behavior:

1. **MapDecorationMixin**: Intercepts off-map player decoration creation and converts them to regular player decorations with correct rotation
2. **MapStateMixin**: Ensures a player decoration always exists, creating one at the map edge if necessary
3. **Smart Caching**: Caches decoration types to minimize registry lookups
4. **Edge Calculation**: Uses mathematical positioning to place decorations at appropriate map edges

### Performance Considerations

- Decoration types are cached after first lookup
- Calculations are only performed when necessary
- Minimal memory footprint
- No impact on server performance (client-side only)

## 🔄 Compatibility

- **Server Compatibility**: Works on any Minecraft server (vanilla, Fabric, Forge, Paper, etc.)
- **Multiplayer**: Fully compatible with multiplayer environments
- **Other Mods**: Should be compatible with most other mods that don't heavily modify map rendering

## ⚡ Performance

- **Minimal Overhead**: Smart caching prevents repeated registry lookups
- **Lazy Processing**: Decorations are only modified when necessary
- **Thread-Safe**: Uses atomic references for safe concurrent access
- **Memory Efficient**: No unnecessary object creation in hot paths
- **Client-Side Only**: Zero server performance impact

## 🐛 Troubleshooting

### Common Issues

**Player decoration not showing**
- Verify you're using a compatible Minecraft version (1.21.5)
- Check that Fabric API is properly installed
- Make sure you're looking at a map item, not just the world map

### Getting Help

If you encounter issues:
1. Check the latest logs for error messages
2. Verify all dependencies are correctly installed
3. Try with minimal mods to isolate conflicts
4. Report issues on the [GitHub Issues page](https://github.com/mrerenk/MapDistanceFix/issues)

## 🏗️ Building from Source

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

## 🤝 Contributing

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

## 📄 License

This project is licensed under the MIT License. See [LICENSE](LICENSE) for details.

## 🙏 Acknowledgments

- **Fabric Team** for the excellent modding framework
- **Minecraft Community** for inspiration and feedback
- **Contributors** who help improve this mod

---

**Made with ❤️ by MrErenK for the Minecraft community**

*For questions, suggestions, or support, feel free to open an issue on GitHub!*

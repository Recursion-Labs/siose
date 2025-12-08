# Siose Minecraft Plugin

A Minecraft plugin that integrates with the BrickChain platform for property management and user authentication.

## Features

- **User Authentication**: Register, login, and logout with BrickChain API
- **Property Requests**: Define property areas using enchanted fence blocks
- **API Integration**: Seamless communication with BrickChain backend
- **Unbreakable Markers**: Property boundary blocks cannot be broken

## Installation

1. Download the latest JAR file from releases
2. Place the JAR in your server's `plugins` folder
3. Restart the server
4. Configure the plugin if needed (optional config in `plugins/Siose/config.yml`)

## Dependencies

- Minecraft Server (Paper/Spigot 1.20+)
- BrickChain API Server running on localhost:3000

## Usage

### Commands

- `/register` - Register for a BrickChain account
- `/login <token>` - Login with your authentication token
- `/logout` - Logout and clear session
- `/getdata` - Fetch user data (requires authentication)
- `/requestproperty` - Get 3 enchanted fence blocks to define property area

### Property Definition

1. Run `/requestproperty` to receive 3 enchanted fence blocks
2. Place the first block at the property corner
3. Place the second block to define length
4. Place the third block to define breadth
5. The plugin calculates the area and submits the property request to the API

## API Integration

The plugin communicates with the BrickChain API at `http://host.docker.internal:3000/v1`:

### Endpoints Used

- `POST /register` - User registration
- `POST /auth/minecraft/login` - Minecraft login
- `GET /user/@me` - Fetch user profile
- `POST /user/property-requests` - Submit property requests

### Property Request Format

```json
{
  "entity": "{\"coordinates\": [[x1,y1,z1], [x2,y2,z2], [x3,y3,z3]], \"area\": 123}"
}
```

## Development

### Prerequisites

- Java 17+
- Maven 3.6+
- Minecraft development environment

### Building

```bash
mvn clean compile
mvn package
```

### Project Structure

```
src/main/java/me/samarthh/
├── Main.java                 # Plugin main class
├── api/
│   └── SioseApiClient.java   # API client for BrickChain
├── commands/                 # Command implementations
├── listeners/                # Event listeners
└── managers/                 # Data managers
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Support

For issues and questions, please open an issue on GitHub or contact the development team.
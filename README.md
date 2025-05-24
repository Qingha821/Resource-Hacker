# Resource Hacker

A lightweight Minecraft mod that enables encrypted server resource packs, automatically loading them when clients join the server.

## 🔐 Important Security Note

**Before using this mod:**
1. You **must** configure an AES secret in the config file
2. The server **may crash on first startup** - this is normal  
   Simply restart after setting your password in the config file

> Default Password: `Sp@admin666` *(Change this immediately!)*

## 🛠 Installation & Usage

### Server Setup
1. Install the mod on your Minecraft server
2. Place your resource pack (ZIP file) in the server's root directory

### Encryption Process
1. Run the command:  
   ```bash
   res-encrypt your-file.zip
   ```
2. This will generate an encrypted file: `your-file.zip.out`
3. Rename this to `your-file.zip`

### Deployment
1. Upload the encrypted ZIP file to your preferred hosting service
2. Configure the download URL in `server.properties`

### Client Setup
1. Install the mod on the client
2. Join the server - the resource pack will load automatically

## 📝 Notes
- The encrypted pack behaves like a normal resource pack for clients
- Ensure your hosting service supports the file type and provides direct download links

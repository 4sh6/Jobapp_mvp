# Gmail SMTP Configuration Guide for OTP Email

## Step 1: Generate Gmail App Password

Gmail no longer allows direct email/password authentication. You must use an **App Password**:

### Prerequisites:
- A Google Account
- 2-Factor Authentication (2FA) enabled on your Google Account

### Steps to Generate App Password:

1. Go to [Google Account Security](https://myaccount.google.com/security)
2. Scroll down to **"How you sign in to Google"**
3. Click on **"2-Step Verification"** and complete the setup if not already done
4. Go back to Security settings
5. Scroll to **"App passwords"** (only visible if 2FA is enabled)
6. Select **"Mail"** and **"Windows Computer"** (or your OS)
7. Google will generate a 16-character password
8. Copy this password

## Step 2: Set Environment Variables

### On macOS/Linux:

```bash
# Option 1: Export in terminal (temporary - only for current session)
export MAIL_USERNAME="your-email@gmail.com"
export MAIL_PASSWORD="your-16-char-app-password"

# Then run the application
./mvnw spring-boot:run
```

### Option 2: Create a `.env` file (Persistent)

Create a file called `.env` in the project root:

```properties
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-16-char-app-password
DB_URL=jdbc:h2:mem:testdb
DB_USERNAME=sa
DB_PASSWORD=
```

Then run with:
```bash
export $(cat .env | xargs) && ./mvnw spring-boot:run
```

### Option 3: Pass as JVM arguments

```bash
./mvnw spring-boot:run \
  -Dspring.mail.username="your-email@gmail.com" \
  -Dspring.mail.password="your-16-char-app-password"
```

⚠️ **Security Warning:** Never hardcode actual passwords in commands or files!

## Step 3: Verify Configuration

When the application starts, you should see:
- **SUCCESS**: "OTP for {email} is {code}" in logs → Email service is configured
- **FALLBACK**: "OTP for {email} is {code} (mailSender not configured)" → Environment variables not set

## Step 4: Test Registration Flow

1. Register a new jobseeker at `http://localhost:8080/jobseeker/register`
2. Check application logs for the OTP (whether sent via email or printed to console)
3. Verify OTP on the OTP verification page

## Troubleshooting

### Issue: "Authentication failed" on registration

**Causes:**
- Duplicate email already exists in database
- Environment variables not properly set

**Solution:**
- Check logs for exact error message
- Use a new email address
- Verify environment variables are set: `echo $MAIL_USERNAME`

### Issue: OTP not appearing in logs

**Causes:**
- Application not restarted after setting environment variables
- Environment variables set in one terminal, app running in another

**Solution:**
- Set environment variables in the SAME terminal before running mvnw
- Or use Spring profiles with application-mail.properties

### Issue: Email not received

**Causes:**
- Gmail SMTP credentials incorrect
- Gmail blocking less secure apps (use App Password instead)
- Network/firewall issues

**Solution:**
- Double-check App Password (16 characters, with spaces removed)
- Try sending test email first:
```bash
curl -X POST http://localhost:8080/test-email \
  -H "Content-Type: application/json" \
  -d '{"to":"recipient@gmail.com", "otp":"123456"}'
```

## Security Notes

⚠️ **NEVER commit credentials to Git!**

- `.env` file should be in `.gitignore`
- Environment variables are read at application startup
- In production, use secrets management (AWS Secrets Manager, Azure Key Vault, etc.)

## Production Deployment

For production, use environment variables or configuration server:

```bash
# AWS Lambda/ECS
MAIL_USERNAME=secret-manager-reference
MAIL_PASSWORD=secret-manager-reference

# Docker
docker run -e MAIL_USERNAME="..." -e MAIL_PASSWORD="..." myapp:latest
```

## Email Service Implementation Details

The `EmailService` class handles:
- **Fallback logging**: If mailSender is null, prints OTP to console (dev mode)
- **Error handling**: If email fails, still logs OTP so verification works
- **3 retry timeouts**: 5s connection, 3s socket, 5s write (Gmail SMTP stability)

See: `src/main/java/com/example/service/EmailService.java`


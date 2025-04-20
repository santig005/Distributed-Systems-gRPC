import winston from 'winston';
import path from 'path';
import fs from 'fs';

// Crea carpeta logs si no existe
const logDir = path.resolve('logs');
if (!fs.existsSync(logDir)) fs.mkdirSync(logDir);

// Configura logger
const logger = winston.createLogger({
  level: 'info',
  format: winston.format.combine(
    winston.format.timestamp(),
    winston.format.printf(({ timestamp, level, message }) => {
      return `[${timestamp}] [${level.toUpperCase()}]: ${message}`;
    })
  ),
  transports: [
    new winston.transports.Console(), // logs a consola
    new winston.transports.File({
      filename: path.join(logDir, 'order_service.log'),
    }), // archivo
  ],
});

export default logger;

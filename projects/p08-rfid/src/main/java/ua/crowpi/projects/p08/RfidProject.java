package ua.crowpi.projects.p08;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ua.crowpi.core.CrowPiProject;
import ua.crowpi.core.exception.HardwareException;
import ua.crowpi.core.hardware.GpioFacade;
import ua.crowpi.core.hardware.I2cFacade;
import ua.crowpi.core.matrix.MatrixScrollerComponent;
import ua.crowpi.core.mock.MockGpioFacade;
import ua.crowpi.core.mock.MockI2cFacade;

/**
 * RFID Door Lock — CrowPi educational project p08.
 *
 * <p>Demonstrates SPI communication with the MFRC522 module, HashMap-based
 * access control, Log4j2 structured logging, and relay-controlled door lock.</p>
 *
 * <p>In mock mode cycles through three pre-seeded UIDs every 3 seconds so the
 * full GRANTED / DENIED / UNKNOWN_CARD flow is visible without hardware.</p>
 */
public class RfidProject implements CrowPiProject {

    private static final Logger LOG = LogManager.getLogger(RfidProject.class);

    /** Interval between mock card scans, in milliseconds. */
    private static final long MOCK_SCAN_INTERVAL_MS = 3_000L;

    /** Pause between processing one card and showing "SCAN CARD..." again. */
    private static final long POST_RESULT_PAUSE_MS = 2_000L;

    // -------------------------------------------------------------------------
    // Runtime state
    // -------------------------------------------------------------------------

    private volatile boolean running = false;

    private GpioFacade gpio;
    private I2cFacade  lcd;

    private MatrixScrollerComponent matrixScroller;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /** Creates an RfidProject for production use; hardware facades created in run(). */
    public RfidProject() { }

    /**
     * Creates an RfidProject with pre-injected facades — for unit testing.
     *
     * @param gpio GPIO facade (may be a mock)
     * @param lcd  I2C facade for the LCD (may be a mock)
     */
    public RfidProject(GpioFacade gpio, I2cFacade lcd) {
        this.gpio = gpio;
        this.lcd  = lcd;
    }

    // -------------------------------------------------------------------------
    // CrowPiProject contract
    // -------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public String getName() { return "RFID Door Lock"; }

    /** {@inheritDoc} */
    @Override
    public String getProjectId() { return "p08"; }

    /** {@inheritDoc} */
    @Override
    public String getDescription() {
        return "MFRC522 RFID reader with in-memory card database, relay door lock, and Log4j2 access logging.";
    }

    /**
     * {@inheritDoc}
     *
     * <p>Initialises hardware facades, builds the card database, then enters a
     * read-process loop.  In mock mode the reader cycles through the three
     * pre-seeded UIDs automatically.</p>
     */
    @Override
    public void run(boolean mockMode) throws HardwareException {
        running = true;
        LOG.info("RfidProject starting (mockMode={})", mockMode);

        // Ініціалізуємо hardware-фасади — реальні або mock залежно від режиму
        boolean matrixMock = (gpio != null) || mockMode;
        if (gpio == null) {
            if (mockMode) {
                gpio = new MockGpioFacade();
                lcd  = new MockI2cFacade();
            } else {
                throw new HardwareException("Real GPIO/I2C facades not yet implemented. Use --mock.");
            }
        }

        // ── LED matrix scroller ─────────────────────────────────────────────────
        matrixScroller = new MatrixScrollerComponent(matrixMock);
        matrixScroller.start();

        CardDatabase       db     = new CardDatabase();
        AccessAttemptLogger logger = new AccessAttemptLogger();
        RelayDoorLock      lock   = new RelayDoorLock(gpio);
        AccessController   ctrl   = new AccessController(db, lock, gpio, lcd, logger);
        RfidReader         reader = new RfidReader(gpio, mockMode);

        // Показуємо стартове повідомлення на LCD
        showStandby(ctrl);

        LOG.info("RFID main loop started");
        while (running) {
            try {
                // Намагаємось прочитати UID — у mock-режимі повертає циклічні UIDs
                String uid = reader.readUid();

                if (uid != null && !uid.isEmpty()) {
                    LOG.debug("Card scanned: {}", uid);
                    ctrl.processCard(uid);

                    // Пауза після результату — даємо час прочитати повідомлення на LCD
                    sleepMs(POST_RESULT_PAUSE_MS);
                    showStandby(ctrl);
                }

                // Невелика затримка між скануваннями — уникаємо busy-loop
                sleepMs(mockMode ? MOCK_SCAN_INTERVAL_MS : 200L);

            } catch (Exception e) {
                LOG.error("Error during RFID scan loop", e);
                sleepMs(1_000L);
            }
        }

        LOG.info("RfidProject main loop exited");
    }

    /** {@inheritDoc} */
    @Override
    public void shutdown() {
        if (!running) return;
        running = false;
        if (gpio != null) gpio.close();
        LOG.info("RfidProject shutdown complete");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Displays the standby "SCAN CARD..." prompt on both LCD rows.
     *
     * @param ctrl the AccessController that owns the LCD facade
     */
    private void showStandby(AccessController ctrl) {
        // Повторно використовуємо lcdShow через AccessController — він вже знає адресу LCD
        // Викликаємо processCard з порожнім UID для відображення standby — ні, краще
        // повторно написати безпосередньо через те, що processCard має side effects.
        // Відображаємо напряму через внутрішній метод-помічник.
        lcdWriteDirect("SCAN CARD...    ", "                ");
    }

    /**
     * Writes two lines directly to the LCD via the I2C facade.
     *
     * @param line1 top row content (padded to 16 chars)
     * @param line2 bottom row content (padded to 16 chars)
     */
    private void lcdWriteDirect(String line1, String line2) {
        if (lcd == null) return;
        writeLine(0, line1);
        writeLine(1, line2);
    }

    private void writeLine(int row, String text) {
        byte rowCmd = (row == 0) ? (byte) 0x80 : (byte) 0xC0;
        lcd.writeByte(AccessController.LCD_ADDR, 0x00, rowCmd);
        String padded = String.format("%-16.16s", text != null ? text : "");
        for (char c : padded.toCharArray()) {
            lcd.writeByte(AccessController.LCD_ADDR, 0x01, (byte) c);
        }
    }

    private static void sleepMs(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

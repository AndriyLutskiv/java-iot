package ua.crowpi.core.pi4j;

import com.pi4j.io.spi.SpiChannel;
import com.pi4j.io.spi.SpiDevice;
import com.pi4j.io.spi.SpiFactory;
import ua.crowpi.core.hardware.SpiFacade;

import java.io.IOException;

/**
 * Production implementation of {@link SpiFacade} using Pi4J v1.4.
 *
 * <p>Opens one of the two SPI0 chip-select channels available on the
 * Raspberry Pi 3 40-pin header:</p>
 * <ul>
 *   <li>Channel 0 — CE0, BCM 8, physical pin 24 ({@code /dev/spidev0.0})</li>
 *   <li>Channel 1 — CE1, BCM 7, physical pin 26 ({@code /dev/spidev0.1})</li>
 * </ul>
 *
 * <p>Pi4J delegates to WiringPi's {@code wiringPiSPIDataRW}, which performs a
 * full-duplex transfer by modifying the data buffer in-place. Because neither
 * the MAX7219 LED-matrix driver nor the MCP3008 ADC returns meaningful MISO
 * data during normal operation, the received bytes are ignored and
 * {@link #transfer(byte[])} always returns a zero-filled array of the same
 * length as the input.</p>
 *
 * <p><strong>Runs only on Raspberry Pi.</strong> Instantiating this class on a
 * desktop will throw {@link HardwareException}.</p>
 */
public final class Pi4jSpiFacade implements SpiFacade {

    /** Default SPI clock rate — 1 MHz, well within MAX7219's 10 MHz maximum. */
    private static final int SPI_SPEED_HZ = 1_000_000;

    private final SpiDevice device;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Opens the SPI device on the given chip-select channel.
     *
     * @param channel 0 for CE0 (physical pin 24) or 1 for CE1 (physical pin 26)
     * @throws HardwareException if the SPI device cannot be opened
     */
    public Pi4jSpiFacade(int channel) {
        SpiChannel spiChannel = (channel == 0) ? SpiChannel.CS0 : SpiChannel.CS1;
        try {
            device = SpiFactory.getInstance(spiChannel, SPI_SPEED_HZ);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Cannot open SPI channel " + channel + ": " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // SpiFacade
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Writes {@code data} to the SPI device via Pi4J / WiringPi.
     * The MISO bytes returned by the device are discarded; the method always
     * returns a zero-filled array of the same length as the input.</p>
     */
    @Override
    public byte[] transfer(byte[] data) {
        try {
            device.write(data, 0, data.length);
        } catch (IOException e) {
            throw new RuntimeException("SPI transfer failed: " + e.getMessage(), e);
        }
        return new byte[data.length];
    }

    /**
     * {@inheritDoc}
     *
     * <p>Pi4J SpiDevice does not require an explicit close; this method is a
     * no-op but is kept to satisfy the {@link SpiFacade} contract.</p>
     */
    @Override
    public void close() {
        // Pi4j SpiDevice is closed implicitly when the JVM exits
    }
}

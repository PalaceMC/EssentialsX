package com.earth2me.essentials;

import com.google.common.io.Files;
import net.ess3.api.InvalidWorldException;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import java.io.*;
import java.math.BigDecimal;
import java.math.MathContext;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jetbrains.annotations.NotNull;

import static com.earth2me.essentials.I18n.tl;


@SuppressWarnings({"ResultOfMethodCallIgnored", "deprecation"})
public class EssentialsConf extends YamlConfiguration {
    protected static final Logger LOGGER = Logger.getLogger("Essentials");
    protected final File configFile;
    protected String templateName = null;
    protected static final Charset UTF8 = StandardCharsets.UTF_8;
    private final Class<?> resourceClass = EssentialsConf.class;
    private static final ExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadExecutor();
    private final AtomicInteger pendingDiskWrites = new AtomicInteger(0);
    private final AtomicBoolean transaction = new AtomicBoolean(false);

    public EssentialsConf(final File configFile) {
        super();
        this.configFile = configFile.getAbsoluteFile();
    }

    private final byte[] bytebuffer = new byte[1024];

    public synchronized void load() {
        if (pendingDiskWrites.get() != 0) {
            LOGGER.log(Level.INFO, "File {0} not read, because it''s not yet written to disk.", configFile);
            return;
        }
        if (!configFile.getParentFile().exists()) {
            if (!configFile.getParentFile().mkdirs()) {
                LOGGER.log(Level.SEVERE, tl("failedToCreateConfig", configFile.toString()));
            }
        }
        // This will delete files where the first character is 0. In most cases they are broken.
        if (configFile.exists() && configFile.length() != 0) {
            try {
                final InputStream input = new FileInputStream(configFile);
                try {
                    if (input.read() == 0) {
                        input.close();
                        configFile.delete();
                    }
                } catch (IOException ex) {
                    LOGGER.log(Level.SEVERE, null, ex);
                } finally {
                    try {
                        input.close();
                    } catch (IOException ex) {
                        LOGGER.log(Level.SEVERE, null, ex);
                    }
                }
            } catch (FileNotFoundException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        }

        if (!configFile.exists()) {
            if (legacyFileExists()) {
                convertLegacyFile();
            } else if (altFileExists()) {
                convertAltFile();
            } else if (templateName != null) {
                LOGGER.log(Level.INFO, tl("creatingConfigFromTemplate", configFile.toString()));
                createFromTemplate();
            } else {
                return;
            }
        }


        try {
            try (FileInputStream inputStream = new FileInputStream(configFile)) {
                long startSize = configFile.length();
                if (startSize > Integer.MAX_VALUE) {
                    throw new InvalidConfigurationException("File too big");
                }
                ByteBuffer buffer = ByteBuffer.allocate((int) startSize);
                int length;
                while ((length = inputStream.read(bytebuffer)) != -1) {
                    if (length > buffer.remaining()) {
                        ByteBuffer resize = ByteBuffer.allocate(buffer.capacity() + length - buffer.remaining());
                        int resizePosition = buffer.position();
                        // Fix builds compiled against Java 9+ breaking on Java 8
                        buffer.rewind();
                        resize.put(buffer);
                        resize.position(resizePosition);
                        buffer = resize;
                    }
                    buffer.put(bytebuffer, 0, length);
                }
                buffer.rewind();
                final CharBuffer data = CharBuffer.allocate(buffer.capacity());
                CharsetDecoder decoder = UTF8.newDecoder();
                CoderResult result = decoder.decode(buffer, data, true);
                if (result.isError()) {
                    buffer.rewind();
                    data.clear();
                    LOGGER.log(Level.INFO, "File " + configFile.getAbsolutePath() + " is not utf-8 encoded, trying " + Charset.defaultCharset().displayName());
                    decoder = Charset.defaultCharset().newDecoder();
                    result = decoder.decode(buffer, data, true);
                    if (result.isError()) {
                        throw new InvalidConfigurationException("Invalid Characters in file " + configFile.getAbsolutePath());
                    } else {
                        decoder.flush(data);
                    }
                } else {
                    decoder.flush(data);
                }
                final int end = data.position();
                data.rewind();
                super.loadFromString(data.subSequence(0, end).toString());
            }
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        } catch (InvalidConfigurationException ex) {
            File broken = new File(configFile.getAbsolutePath() + ".broken." + System.currentTimeMillis());
            configFile.renameTo(broken);
            LOGGER.log(Level.SEVERE, "The file " + configFile.toString() + " is broken, it has been renamed to " + broken.toString(), ex.getCause());
        }
    }

    public boolean legacyFileExists() {
        return false;
    }

    public void convertLegacyFile() {
        LOGGER.log(Level.SEVERE, "Unable to import legacy config file.");
    }

    public boolean altFileExists() {
        return false;
    }

    public void convertAltFile() {
        LOGGER.log(Level.SEVERE, "Unable to import alt config file.");
    }

    private void createFromTemplate() {
        InputStream istr = null;
        OutputStream ostr = null;
        try {
            istr = resourceClass.getResourceAsStream(templateName);
            if (istr == null) {
                LOGGER.log(Level.SEVERE, tl("couldNotFindTemplate", templateName));
                return;
            }
            ostr = new FileOutputStream(configFile);
            byte[] buffer = new byte[1024];
            int length = istr.read(buffer);
            while (length > 0) {
                ostr.write(buffer, 0, length);
                length = istr.read(buffer);
            }
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, tl("failedToWriteConfig", configFile.toString()), ex);
        } finally {
            try {
                if (istr != null) {
                    istr.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(EssentialsConf.class.getName()).log(Level.SEVERE, null, ex);
            }
            try {
                if (ostr != null) {
                    ostr.close();
                }
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, tl("failedToCloseConfig", configFile.toString()), ex);
            }
        }
    }

    public void setTemplateName(final String templateName) {
        this.templateName = templateName;
    }

    public File getFile() {
        return configFile;
    }

    public void startTransaction() {
        transaction.set(true);
    }

    public void stopTransaction() {
        transaction.set(false);
        save();
    }

    public void save() {
        save(configFile);
    }

    @Override
    public synchronized void save(@NotNull final File file) {
        if (!transaction.get()) {
            delayedSave(file);
        }
    }

    //This may be aborted if there are stagnant requests sitting in queue.
    //This needs fixed to discard outstanding save requests.
    public synchronized void forceSave() {
        try {
            Future<?> future = delayedSave(configFile);
            if (future != null) {
                future.get();
            }
        } catch (InterruptedException | ExecutionException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    public synchronized void cleanup() {
        forceSave();
    }

    private Future<?> delayedSave(final File file) {
        if (file == null) {
            throw new IllegalArgumentException("File cannot be null");
        }

        final String data = saveToString();

        if (data.length() == 0) {
            return null;
        }

        pendingDiskWrites.incrementAndGet();

        return EXECUTOR_SERVICE.submit(new WriteRunner(configFile, data, pendingDiskWrites));
    }


    private static class WriteRunner implements Runnable {
        private final File configFile;
        private final String data;
        private final AtomicInteger pendingDiskWrites;

        private WriteRunner(final File configFile, final String data, final AtomicInteger pendingDiskWrites) {
            this.configFile = configFile;
            this.data = data;
            this.pendingDiskWrites = pendingDiskWrites;
        }

        @Override
        public void run() {
            //long startTime = System.nanoTime();
            synchronized (configFile) {
                if (pendingDiskWrites.get() > 1) {
                    // Writes can be skipped, because they are stored in a queue (in the executor).
                    // Only the last is actually written.
                    pendingDiskWrites.decrementAndGet();
                    return;
                }
                try {
                    //noinspection UnstableApiUsage
                    Files.createParentDirs(configFile);

                    if (!configFile.exists()) {
                        try {
                            LOGGER.log(Level.INFO, tl("creatingEmptyConfig", configFile.toString()));
                            if (!configFile.createNewFile()) {
                                LOGGER.log(Level.SEVERE, tl("failedToCreateConfig", configFile.toString()));
                                return;
                            }
                        } catch (IOException ex) {
                            LOGGER.log(Level.SEVERE, tl("failedToCreateConfig", configFile.toString()), ex);
                            return;
                        }
                    }

                    try (FileOutputStream fos = new FileOutputStream(configFile)) {
                        try (OutputStreamWriter writer = new OutputStreamWriter(fos, UTF8)) {
                            writer.write(data);
                        }
                    }
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, e.getMessage(), e);
                } finally {
                    pendingDiskWrites.decrementAndGet();
                }
            }
        }
    }

    public boolean hasProperty(final String path) {
        return isSet(path);
    }

    public Location getLocation(final String path, final Server server) throws InvalidWorldException {
        final String worldString = (path == null ? "" : path + ".") + "world";
        final String worldName = getString(worldString);
        if (worldName == null || worldName.isEmpty()) {
            return null;
        }
        final World world = server.getWorld(worldName);
        if (world == null) {
            throw new InvalidWorldException(worldName);
        }
        return new Location(
                world,
                getDouble((path == null ? "" : path + ".") + "x", 0),
                getDouble((path == null ? "" : path + ".") + "y", 0),
                getDouble((path == null ? "" : path + ".") + "z", 0),
                (float) getDouble((path == null ? "" : path + ".") + "yaw", 0),
                (float) getDouble((path == null ? "" : path + ".") + "pitch", 0)
        );
    }

    public void setProperty(final String path, final Location loc) {
        World world = loc.getWorld();
        set((path == null ? "" : path + ".") + "world", (world != null) ? world.getName() : "null");
        set((path == null ? "" : path + ".") + "x", loc.getX());
        set((path == null ? "" : path + ".") + "y", loc.getY());
        set((path == null ? "" : path + ".") + "z", loc.getZ());
        set((path == null ? "" : path + ".") + "yaw", loc.getYaw());
        set((path == null ? "" : path + ".") + "pitch", loc.getPitch());
    }

    @Override
    public ItemStack getItemStack(@NotNull final String path) {
        final ItemStack stack = new ItemStack(Material.valueOf(getString(path + ".type", "AIR")), getInt(path + ".amount", 1), (short) getInt(path + ".damage", 0));
        final ConfigurationSection enchants = getConfigurationSection(path + ".enchant");
        if (enchants != null) {
            for (String enchant : enchants.getKeys(false)) {
                final Enchantment enchantment = Enchantment.getByName(enchant.toUpperCase(Locale.ENGLISH));
                if (enchantment == null) {
                    continue;
                }
                final int level = getInt(path + ".enchant." + enchant, enchantment.getStartLevel());
                stack.addUnsafeEnchantment(enchantment, level);
            }
        }
        return stack;
    }

    public void setProperty(final String path, final ItemStack stack) {
        final Map<String, Object> map = new HashMap<>();
        map.put("type", stack.getType().toString());
        map.put("amount", stack.getAmount());
        map.put("damage", stack.getDurability());
        Map<Enchantment, Integer> enchantments = stack.getEnchantments();
        if (!enchantments.isEmpty()) {
            Map<String, Integer> enchant = new HashMap<>();
            for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                enchant.put(entry.getKey().getName().toLowerCase(Locale.ENGLISH), entry.getValue());
            }
            map.put("enchant", enchant);
        }
        set(path, map);
    }

    public void setProperty(String path, List<Object> object) {
        set(path, new ArrayList<>(object));
    }

    public void setProperty(String path, Map<Object, Object> object) {
        set(path, new LinkedHashMap<>(object));
    }

    public Object getProperty(String path) {
        return get(path);
    }

    public void setProperty(final String path, final BigDecimal bigDecimal) {
        set(path, bigDecimal.toString());
    }

    public void setProperty(String path, Object object) {
        set(path, object);
    }

    public void removeProperty(String path) {
        set(path, null);
    }

    public synchronized BigDecimal getBigDecimal(final String path, final BigDecimal def) {
        final String input = super.getString(path);
        return toBigDecimal(input, def);
    }

    public static BigDecimal toBigDecimal(final String input, final BigDecimal def) {
        if (input == null || input.isEmpty()) {
            return def;
        } else {
            try {
                return new BigDecimal(input, MathContext.DECIMAL128);
            } catch (NumberFormatException | ArithmeticException e) {
                return def;
            }
        }
    }
}

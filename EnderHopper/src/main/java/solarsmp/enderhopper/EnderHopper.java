package solarsmp.enderhopper;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Hopper;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class EnderHopper extends JavaPlugin implements Listener {
    int[][] getAllHopperInputPositions() {
        String[] hopperPositionsStringArray = hopperInputHash.keySet().toArray(new String[0]);
        int ArraySize = hopperPositionsStringArray.length;

        int[][] hopperPositions = new int[ArraySize][3];
        for (int i = 0; i <= ArraySize - 1; ++i) {
            hopperPositions[i][0] = Integer.parseInt(hopperPositionsStringArray[i].split("\\s")[0]);
            hopperPositions[i][1] = Integer.parseInt(hopperPositionsStringArray[i].split("\\s")[1]);
            hopperPositions[i][2] = Integer.parseInt(hopperPositionsStringArray[i].split("\\s")[2]);
        }

        return hopperPositions;
    }

    int[][] getAllHopperOutputPositions() {
        String[] hopperPositionsStringArray = hopperOutputHash.keySet().toArray(new String[0]);
        int ArraySize = hopperPositionsStringArray.length;

        int[][] hopperPositions = new int[ArraySize][3];
        for (int i = 0; i <= ArraySize - 1; ++i) {
            hopperPositions[i][0] = Integer.parseInt(hopperPositionsStringArray[i].split("\\s")[0]);
            hopperPositions[i][1] = Integer.parseInt(hopperPositionsStringArray[i].split("\\s")[1]);
            hopperPositions[i][2] = Integer.parseInt(hopperPositionsStringArray[i].split("\\s")[2]);
        }

        return hopperPositions;
    }

    public String readDataFile(String filename) {
        String readData = "";
        try (BufferedReader br = new BufferedReader(new FileReader(getDataFolder() + "/" + filename))) {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
                line = br.readLine();
            }

            readData = sb.toString();
        } catch (IOException e) {
            log.log(Level.WARNING, e.toString());
        }

        return readData;
    }

    public void writeDataFile(String filename, String text) {
        try (PrintWriter writer = new PrintWriter(getDataFolder() + "/" + filename, "UTF-8")) {
            writer.print(text);
        } catch (IOException e) {
            log.log(Level.WARNING, e.toString());
        }
    }

    public String hashMapToJson(HashMap<String, String> map) {
        return new Gson().toJson(map);
    }

    public HashMap<String, String> JsonToHashMap(String text) {
        return new Gson().fromJson(text, new TypeToken<HashMap<String, String>>() {
        }.getType());
    }

    public String format_coords(Block block) {
        return block.getX() + " " + block.getY() + " " + block.getZ();
    }

    public String format_coords(int[] coords) {
        return coords[0] + " " + coords[1] + " " + coords[2];
    }

    public int[] unformat_coords(String coords) {
        int[] result = new int[3];

        result[0] = Integer.parseInt(coords.split("\\s")[0]);
        result[1] = Integer.parseInt(coords.split("\\s")[1]);
        result[2] = Integer.parseInt(coords.split("\\s")[2]);

        return result;
    }

    public int[] get_hopper_settings(String hopperSettings) {
        int[] result = new int[2];

        result[0] = Integer.parseInt(hopperSettings.split("\\s")[0]);
        result[1] = Integer.parseInt(hopperSettings.split("\\s")[1]);

        return result;
    }


    private Logger log;
    private HashMap<String, String> enderChestHash = new HashMap<>();
    private HashMap<String, String> hopperInputHash = new HashMap<>();
    private HashMap<String, String> hopperOutputHash = new HashMap<>();
    private HashMap<String, String> linkedInputHopperToEnderChestHash = new HashMap<>();
    private HashMap<String, String> linkedOutputHopperToEnderChestHash = new HashMap<>();


    @Override
    public void onEnable() {
        log = getLogger();

        File dataDir = getDataFolder();
        if (!dataDir.exists()) {
            if (dataDir.mkdir()) {
                writeDataFile("storage1.json", "{}");
                writeDataFile("storage2.json", "{}");
                writeDataFile("storage3.json", "{}");
                writeDataFile("storage4.json", "{}");
                writeDataFile("storage5.json", "{}");

                log.info("Created config directory!");
            }
        }

        enderChestHash = JsonToHashMap(readDataFile("storage1.json"));
        hopperInputHash = JsonToHashMap(readDataFile("storage2.json"));
        hopperOutputHash = JsonToHashMap(readDataFile("storage3.json"));
        linkedInputHopperToEnderChestHash = JsonToHashMap(readDataFile("storage4.json"));
        linkedOutputHopperToEnderChestHash = JsonToHashMap(readDataFile("storage5.json"));

        getServer().getPluginManager().registerEvents(this, this);

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            try {
                int[][] allInputHoppersCoords = getAllHopperInputPositions();
                for (int[] hopperCoords : allInputHoppersCoords) {
                    int[] correspondingEnderChestCoords = unformat_coords(linkedInputHopperToEnderChestHash.get(format_coords(hopperCoords)));
                    Player player = Bukkit.getPlayer(UUID.fromString(enderChestHash.get(format_coords(correspondingEnderChestCoords))));

                    if (player != null) {
                        Hopper hopperBlock = (Hopper) player.getWorld().getBlockAt(hopperCoords[0], hopperCoords[1], hopperCoords[2]).getState();
                        Inventory enderChestInv = player.getEnderChest();
                        Inventory hopperInv = hopperBlock.getInventory();

                        String hopperSettings = hopperInputHash.get(format_coords(hopperCoords));
                        int row = get_hopper_settings(hopperSettings)[0];
                        int col = get_hopper_settings(hopperSettings)[1];

                        if (row == 0 && col == 0) {
                            //ItemStack[] hopperInvContents = hopperInv.getContents();
                            ItemStack hopperFirstItem = null;

                            for (int i = 0; i <= 4 && hopperFirstItem == null; i++) {
                                hopperFirstItem = hopperInv.getItem(i);
                            }

                            if (hopperFirstItem != null) {
                                int hopperFirstItemOriginalAmount = hopperFirstItem.getAmount();

                                hopperInv.remove(hopperFirstItem);

                                hopperFirstItem.setAmount(1);
                                HashMap<Integer, ItemStack> result = enderChestInv.addItem(hopperFirstItem);

                                if (result.isEmpty()) {
                                    hopperFirstItem.setAmount(hopperFirstItemOriginalAmount - 1);
                                    hopperInv.addItem(hopperFirstItem);
                                } else {
                                    Map.Entry<Integer, ItemStack> resultEntry = result.entrySet().iterator().next();

                                    ItemStack returnItems = resultEntry.getValue();
                                    returnItems.setAmount(hopperFirstItemOriginalAmount);

                                    hopperInv.addItem(returnItems);
                                }
                            }
                        }
                    }
                }

                int[][] allOutputHoppersCoords = getAllHopperOutputPositions();
                for (int[] hopperCoords : allOutputHoppersCoords) {
                    int[] correspondingEnderChestCoords = unformat_coords(linkedOutputHopperToEnderChestHash.get(format_coords(hopperCoords)));
                    Player player = Bukkit.getPlayer(UUID.fromString(enderChestHash.get(format_coords(correspondingEnderChestCoords))));

                    if (player != null) {
                        Hopper hopperBlock = (Hopper) player.getWorld().getBlockAt(hopperCoords[0], hopperCoords[1], hopperCoords[2]).getState();
                        Inventory enderChestInv = player.getEnderChest();
                        Inventory hopperInv = hopperBlock.getInventory();

                        String hopperSettings = hopperOutputHash.get(format_coords(hopperCoords));
                        int row = get_hopper_settings(hopperSettings)[0];
                        int col = get_hopper_settings(hopperSettings)[1];

                        if (row == 0 && col == 0) {
                            ItemStack enderChestFirstItem = null;

                            int i;
                            for (i = 0; i <= 26 && enderChestFirstItem == null; i++) {
                                enderChestFirstItem = enderChestInv.getItem(i);
                            }

                            i -= 1;
                            if (enderChestFirstItem != null) {
                                int amount = enderChestFirstItem.getAmount();

                                enderChestInv.clear(i);
                                enderChestFirstItem.setAmount(amount - 1);
                                enderChestInv.setItem(i, enderChestFirstItem);

                                enderChestFirstItem.setAmount(1);
                                HashMap<Integer, ItemStack> result = hopperInv.addItem(enderChestFirstItem);

                                if (!result.isEmpty()) {
                                    Map.Entry<Integer, ItemStack> resultEntry = result.entrySet().iterator().next();
                                    enderChestInv.addItem(resultEntry.getValue());
                                }
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}
        }, 0, 10);
    }

    @Override
    public void onDisable() {
        writeDataFile("storage1.json", hashMapToJson(enderChestHash));
        writeDataFile("storage2.json", hashMapToJson(hopperInputHash));
        writeDataFile("storage3.json", hashMapToJson(hopperOutputHash));
        writeDataFile("storage4.json", hashMapToJson(linkedInputHopperToEnderChestHash));
        writeDataFile("storage5.json", hashMapToJson(linkedOutputHopperToEnderChestHash));
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();

        if (block.getType() == Material.ENDER_CHEST) {
            String playerUUID = event.getPlayer().getUniqueId().toString();

            enderChestHash.put(format_coords(block), playerUUID);
            log.info(String.valueOf(enderChestHash));
        } else if (block.getType() == Material.HOPPER) {
            boolean output = false;

            int row = -2;
            int col = -2;

            Block enderChestBlock;
            if (event.getBlockAgainst().getType() == Material.ENDER_CHEST) {
                enderChestBlock = event.getBlockAgainst();

                if (block.getY() < enderChestBlock.getY()) {
                    output = true;
                }

            } else {
                enderChestBlock = event.getPlayer().getWorld().getBlockAt(block.getX(), block.getY() + 1, block.getZ());
                output = true;
            }

            if (enderChestBlock.getType() == Material.ENDER_CHEST) {
                row = -1;
                col = -1;

                String hopperName = ((Hopper) block.getState()).getCustomName();

                if (hopperName != null) {
                    if (hopperName.equals("Multi hopper")) {
                        row = 0;
                        col = 0;
                    } else if (hopperName.startsWith("ENDER ")) {
                        try {
                            String options = hopperName.split("\\s")[1];
                            int tempRow = Integer.parseInt(options.split(":")[0]);
                            int tempCol = Integer.parseInt(options.split(":")[1]);

                            if ((tempRow > 3 || tempRow < 1) || (tempCol > 9 || tempCol < 1)) {
                                throw new Exception();
                            } else {
                                row = tempRow;
                                col = tempCol;
                            }

                        } catch (Exception ignored) {
                        }
                    }
                }
            }

            if (row == -1) {
                log.info("Hopper won't be used, its not named!");
            } else if (row == 0) {
                if (!output) {
                    hopperInputHash.put(format_coords(block), "0 0");
                    linkedInputHopperToEnderChestHash.put(format_coords(block), format_coords(enderChestBlock));
                } else {
                    hopperOutputHash.put(format_coords(block), "0 0");
                    linkedOutputHopperToEnderChestHash.put(format_coords(block), format_coords(enderChestBlock));
                }

            } else {
                if (row != -2) {
                    log.info("Using slot at row " + row + " and column " + col);

                    if (!output) {
                        hopperInputHash.put(format_coords(block), row + " " + col);
                        linkedInputHopperToEnderChestHash.put(format_coords(block), enderChestBlock.getX() + " " + enderChestBlock.getY() + " " + enderChestBlock.getZ());
                    } else {
                        hopperOutputHash.put(format_coords(block), row + " " + col);
                        linkedOutputHopperToEnderChestHash.put(format_coords(block), enderChestBlock.getX() + " " + enderChestBlock.getY() + " " + enderChestBlock.getZ());
                    }
                }
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();

        if (block.getType() == Material.ENDER_CHEST) {
            enderChestHash.remove(format_coords(block));
        } else if (block.getType() == Material.HOPPER) {
            hopperInputHash.remove(format_coords(block));
            hopperOutputHash.remove(format_coords(block));
            linkedInputHopperToEnderChestHash.remove(format_coords(block));
            linkedOutputHopperToEnderChestHash.remove(format_coords(block));
        }
    }
}
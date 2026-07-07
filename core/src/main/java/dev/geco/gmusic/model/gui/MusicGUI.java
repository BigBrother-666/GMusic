package dev.geco.gmusic.model.gui;

import dev.geco.gmusic.api.event.GMusicReloadEvent;
import dev.geco.gmusic.model.PlayListMode;
import dev.geco.gmusic.model.PlayMode;
import dev.geco.gmusic.model.PlaySettings;
import dev.geco.gmusic.model.PlayState;
import dev.geco.gmusic.model.Song;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import dev.geco.gmusic.GMusicMain;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class MusicGUI {

	private final GMusicMain gMusicMain = GMusicMain.getInstance();
	private final HashMap<Integer, Song> pageSongs = new HashMap<>();
	private static final HashMap<UUID, MusicGUI> musicGUIS = new HashMap<>();
	private static final int VOLUME_STEPS = 10;
	private static final int SHIFT_VOLUME_STEPS = 1;
	private static final long RANGE_STEPS = 1;
	private static final long SHIFT_RANGE_STEPS = 10;
	private final UUID uuid;
	private final MenuType type;
	private final Inventory inventory;
	private final Listener listener;
	private boolean optionState = false;
	private int page = 1;
	private boolean searchMode = false;
	private String searchKey = null;
	private final PlaySettings playSettings;

	public MusicGUI(@NotNull UUID uuid, @NotNull MenuType type) {
		this.uuid = uuid;
		this.type = type;

		musicGUIS.put(uuid, this);

		playSettings = gMusicMain.getPlaySettingsService().getPlaySettings(uuid);
		inventory = Bukkit.createInventory(new InventoryHolder() {

			@Override
			public @NotNull Inventory getInventory() { return inventory; }

		}, 6 * 9, gMusicMain.getMessageService().getMessage(type == MenuType.RADIO ? "MusicGUI.radio-title" : "MusicGUI.title"));

		setPage(1);

		setDefaultBar();

		listener = new Listener() {

			@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
			public void ICliE(InventoryClickEvent event) {
				if(!event.getInventory().equals(inventory)) return;
				ClickType click = event.getClick();
				if(gMusicMain.getVersionService().executeMethod(event.getView(), "getBottomInventory").equals(event.getClickedInventory())) {
					switch(click) {
						case SHIFT_RIGHT:
						case SHIFT_LEFT:
							event.setCancelled(true);
							break;
					}
					return;
				}
				if(!gMusicMain.getVersionService().executeMethod(event.getView(), "getTopInventory").equals(event.getClickedInventory())) return;
				event.setCancelled(true);
				ItemStack itemStack = event.getCurrentItem();
				if(itemStack == null) return;
				ItemMeta itemMeta = itemStack.getItemMeta();
				HumanEntity clicker = event.getWhoClicked();
				int slot = event.getRawSlot();
				switch(slot) {
					case 45 -> {
						if(!optionState) {
							PlayState songSettings = gMusicMain.getPlayService().getPlayState(uuid);
							if(songSettings == null) return;
							switch(type) {
								case DEFAULT -> {
									Player target = Bukkit.getPlayer(uuid);
									if(target == null) return;
									if(songSettings.isPaused()) gMusicMain.getPlayService().resumeSong(target);
									else gMusicMain.getPlayService().pauseSong(target);
								}
								case JUKEBOX -> {
									if(songSettings.isPaused()) gMusicMain.getJukeBoxService().resumeBoxSong(uuid);
									else gMusicMain.getJukeBoxService().pauseBoxSong(uuid);
								}
								case RADIO -> {
									if(songSettings.isPaused()) gMusicMain.getRadioService().resumeSong();
									else gMusicMain.getRadioService().pauseSong();
								}
							}
						} else {
							setDefaultBar();
						}
					}
					case 46 -> {
						if(!optionState) {
							switch(type) {
								case DEFAULT -> {
									Player target = Bukkit.getPlayer(uuid);
									if(target == null) return;
									gMusicMain.getPlayService().stopSong(target);
								}
								case JUKEBOX -> {
									gMusicMain.getJukeBoxService().stopBoxSong(uuid);
								}
								case RADIO -> {
									gMusicMain.getRadioService().stopSong();
								}
							}
						} else if(type != MenuType.RADIO) {
							int volumn = playSettings.getVolume();
							int step = click == ClickType.SHIFT_LEFT || click == ClickType.SHIFT_RIGHT ? SHIFT_VOLUME_STEPS : VOLUME_STEPS;
							int newVolumn = click == ClickType.MIDDLE ? gMusicMain.getConfigService().PS_D_VOLUME : (click == ClickType.RIGHT ? Math.max(volumn - step, 0) : Math.min(volumn + step, 100));
							playSettings.setVolume(newVolumn);
							itemMeta.setDisplayName(gMusicMain.getMessageService().getMessage("MusicGUI.music-options-volume", "%Volume%", "" + newVolumn));
						}
					}
					case 47 -> {
						if(!optionState) {
							switch(type) {
								case DEFAULT -> {
									Player target = Bukkit.getPlayer(uuid);
									if(target == null) return;
									gMusicMain.getPlayService().playSong(target, gMusicMain.getPlayService().getNextSong(target));
								}
								case JUKEBOX -> {
									gMusicMain.getJukeBoxService().playBoxSong(uuid, gMusicMain.getJukeBoxService().getNextSong(uuid));
								}
								case RADIO -> {
									gMusicMain.getRadioService().playSong(gMusicMain.getRadioService().getNextSong());
								}
							}
						} else if(type != MenuType.RADIO) {
							playSettings.setShowParticles(click == ClickType.MIDDLE ? gMusicMain.getConfigService().PS_D_PARTICLES : !playSettings.isShowingParticles());
							itemMeta.setDisplayName(gMusicMain.getMessageService().getMessage("MusicGUI.music-options-particle", "%Particle%", gMusicMain.getMessageService().getMessage(playSettings.isShowingParticles() ? "MusicGUI.music-options-true" : "MusicGUI.music-options-false")));
						}
					}
					case 48 -> {
						if(playSettings.getPlayListMode() == PlayListMode.RADIO) return;
						if(!optionState) {
							if(gMusicMain.getConfigService().G_DISABLE_RANDOM_SONG) return;
							switch(type) {
								case DEFAULT -> {
									Player target = Bukkit.getPlayer(uuid);
									if(target == null) return;
									gMusicMain.getPlayService().playSong(target, gMusicMain.getPlayService().getRandomSong(uuid));
								}
								case JUKEBOX -> {
									gMusicMain.getJukeBoxService().playBoxSong(uuid, gMusicMain.getPlayService().getRandomSong(uuid));
								}
								case RADIO -> {
									gMusicMain.getRadioService().playSong(gMusicMain.getPlayService().getRandomSong(gMusicMain.getRadioService().getRadioUUID()));
								}
							}
						} else if(type != MenuType.RADIO) {
							playSettings.setPlayOnJoin(click == ClickType.MIDDLE ? gMusicMain.getConfigService().R_PLAY_ON_JOIN : !playSettings.isPlayOnJoin());
							itemMeta.setDisplayName(gMusicMain.getMessageService().getMessage("MusicGUI.music-options-join", "%Join%", gMusicMain.getMessageService().getMessage(playSettings.isPlayOnJoin() ? "MusicGUI.music-options-true" : "MusicGUI.music-options-false")));
						}
					}
					case 49 -> {
						if(type == MenuType.RADIO) return;
						if(!optionState) {
							int playListModeId = playSettings.getPlayListMode().getId();
							PlayListMode playListMode = PlayListMode.byId(click == ClickType.MIDDLE ? gMusicMain.getConfigService().PS_D_PLAYLIST_MODE : (click == ClickType.RIGHT ? (playListModeId - 1 < 0 ? PlayListMode.values().length - 1 : playListModeId - 1) : (playListModeId + 1 > PlayListMode.values().length - 1 ? 0 : playListModeId + 1)));
							playSettings.setPlayListMode(playListMode);
							switch(type) {
								case DEFAULT -> {
									Player target = Bukkit.getPlayer(uuid);
									if(playListMode.getId() != playListModeId && target != null) {
										setPage(1);
										gMusicMain.getPlayService().stopSong(target);
									}
									if(playListMode == PlayListMode.RADIO) {
										gMusicMain.getRadioService().addRadioPlayer(target);
									} else {
										gMusicMain.getRadioService().removeRadioPlayer(target);
									}
								}
								case JUKEBOX -> {
									if(playListMode.getId() != playListModeId) {
										setPage(1);
										gMusicMain.getJukeBoxService().stopBoxSong(uuid);
									}
									if(playListMode == PlayListMode.RADIO) {
										gMusicMain.getRadioService().addRadioJukeBox(uuid, gMusicMain.getJukeBoxService().getJukeBoxBlock(uuid));
									} else {
										gMusicMain.getRadioService().removeRadioJukeBox(uuid);
									}
								}
							}
							setDefaultBar();
						} else {
							if(playSettings.getPlayListMode() == PlayListMode.RADIO) return;
							int playModeId = playSettings.getPlayMode().getId();
							PlayMode playMode = PlayMode.byId(click == ClickType.MIDDLE ? gMusicMain.getConfigService().PS_D_PLAY_MODE : (click == ClickType.RIGHT ? (playModeId - 1 < 0 ? PlayMode.values().length - 1 : playModeId - 1) : (playModeId + 1 > PlayMode.values().length - 1 ? 0 : playModeId + 1)));
							playSettings.setPlayMode(playMode);
							itemMeta.setDisplayName(gMusicMain.getMessageService().getMessage(playMode == PlayMode.DEFAULT ? "MusicGUI.music-options-play-mode-once" : playMode == PlayMode.SHUFFLE ? "MusicGUI.music-options-play-mode-shuffle" : "MusicGUI.music-options-play-mode-repeat"));
						}
					}
					case 50 -> {
						if(!optionState) {
							setOptionsBar();
						} else {
							if(playSettings.getPlayListMode() == PlayListMode.RADIO) return;
							playSettings.setReverseMode(click == ClickType.MIDDLE ? gMusicMain.getConfigService().PS_D_REVERSE : !playSettings.isReverseMode());
							itemMeta.setDisplayName(gMusicMain.getMessageService().getMessage("MusicGUI.music-options-reverse", "%Reverse%", gMusicMain.getMessageService().getMessage(playSettings.isReverseMode() ? "MusicGUI.music-options-true" : "MusicGUI.music-options-false")));
						}
					}
					case 51 -> {
						if(!optionState) {
							if(playSettings.getPlayListMode() == PlayListMode.RADIO) return;
							if(!gMusicMain.getVersionService().isAvailable()) return;
							if(click == ClickType.LEFT) {
								MusicInputGUI inputGUI = getInputGUIInstance((input) -> {
									searchKey = input;
									setPage(1);
									setDefaultBar();
									clicker.openInventory(inventory);
									searchMode = false;
									return true;
								}, ItemMeta::getDisplayName);
								ItemStack nameItem = new ItemStack(Material.NAME_TAG);
								ItemMeta nameItemMeta = nameItem.getItemMeta();
								nameItemMeta.setDisplayName(gMusicMain.getMessageService().getMessage("MusicGUI.music-search-menu-field"));
								nameItem.setItemMeta(nameItemMeta);
								searchKey = null;
								searchMode = true;
								inputGUI.open(clicker, gMusicMain.getMessageService().getMessage("MusicGUI.music-search-menu-title"), nameItem);
							} else if(searchKey != null) {
								searchKey = null;
								setPage(1);
								setDefaultBar();
							}
						} else {
							if(type != MenuType.JUKEBOX) return;
							long range = playSettings.getRange();
							long step = click == ClickType.SHIFT_LEFT || click == ClickType.SHIFT_RIGHT ? SHIFT_RANGE_STEPS : RANGE_STEPS;
							long newRange = click == ClickType.MIDDLE ? gMusicMain.getConfigService().JUKEBOX_RANGE : (click == ClickType.RIGHT ? Math.max(range - step, 0) : Math.min(range + step, gMusicMain.getConfigService().MAX_JUKEBOX_RANGE));
							playSettings.setRange(newRange);
							itemMeta.setDisplayName(gMusicMain.getMessageService().getMessage("MusicGUI.music-options-range", "%Range%", "" + newRange));
						}
					}
					case 52 -> setPage(page - 1);
					case 53 -> setPage(page + 1);
					default -> {
						if(slot < 0 || slot > 44) return;
						Song song = pageSongs.get(slot);
						if(song == null) return;
						if(click == ClickType.MIDDLE) {
							if(playSettings.getFavorites().contains(song)) playSettings.getFavorites().remove(song);
							else playSettings.getFavorites().add(song);
							setPage(page);
							return;
						}
						if(click == ClickType.SHIFT_LEFT && type == MenuType.DEFAULT && gMusicMain.getConfigService().DISC_BUY_ENABLED) {
							Player buyer = Bukkit.getPlayer(uuid);
							if(buyer == null) return;
							buyDisc(buyer, song);
							return;
						}
						switch(type) {
							case DEFAULT -> {
								Player target = Bukkit.getPlayer(uuid);
								if(target == null) return;
								gMusicMain.getPlayService().playSong(target, song);
							}
							case JUKEBOX -> {
								gMusicMain.getJukeBoxService().playBoxSong(uuid, song);
							}
							case RADIO -> {
								gMusicMain.getRadioService().playSong(song);
							}
						}
					}
				}
				setPauseResumeBar();
				itemStack.setItemMeta(itemMeta);
			}

			@EventHandler (ignoreCancelled = true, priority = EventPriority.LOWEST)
			public void inventoryDragEvent(InventoryDragEvent event) {
				if(!event.getInventory().equals(inventory)) return;
				for(int slot : event.getRawSlots()) {
					if(slot >= inventory.getSize()) continue;
					event.setCancelled(true);
					return;
				}
			}

			@EventHandler
			public void inventoryCloseEvent(InventoryCloseEvent Event) { if(Event.getInventory().equals(inventory)) close(false); }

			@EventHandler (ignoreCancelled = true)
			public void gMusicReloadEvent(GMusicReloadEvent Event) { if(Event.getPlugin().equals(gMusicMain)) close(true); }

			@EventHandler
			public void pluginDisableEvent(PluginDisableEvent Event) { if(Event.getPlugin().equals(gMusicMain)) close(true); }
		};

		Bukkit.getPluginManager().registerEvents(listener, gMusicMain);
	}

	public static @Nullable MusicGUI getMusicGUI(@NotNull UUID uuid) { return musicGUIS.get(uuid); }

	public void close(boolean force) {
		if(force) for(HumanEntity entity : new ArrayList<>(inventory.getViewers())) entity.closeInventory();
		if(!force && (searchMode || type == MenuType.JUKEBOX || type == MenuType.RADIO)) return;
		musicGUIS.remove(uuid);
		HandlerList.unregisterAll(listener);
	}

	private @Nullable MusicInputGUI getInputGUIInstance(@NotNull MusicInputGUI.InputCallback call, @NotNull MusicInputGUI.ValidateCallback validateCall) {
		try {
			Class<?> inputGUIClass = Class.forName(gMusicMain.getVersionService().getPackagePath() + ".model.gui.MusicInputGUI");
			return (MusicInputGUI) inputGUIClass.getConstructor(MusicInputGUI.InputCallback.class, MusicInputGUI.ValidateCallback.class).newInstance(call, validateCall);
		} catch(Throwable e) { gMusicMain.getLogger().log(Level.SEVERE, "Could not get input gui instance", e); }
		return null;
	}

	private void clearBar() { for(int slot = 45; slot < 52; slot++) inventory.setItem(slot, null); }

	public void setDefaultBar() {
		optionState = false;

		clearBar();

		ItemStack itemStack;
		ItemMeta itemMeta;

		if(!gMusicMain.getConfigService().G_DISABLE_RANDOM_SONG && playSettings.getPlayListMode() != PlayListMode.RADIO) {
			itemStack = new ItemStack(Material.ENDER_PEARL);
			itemMeta = itemStack.getItemMeta();
			itemMeta.setDisplayName(gMusicMain.getMessageService().getMessage("MusicGUI.music-random"));
			itemStack.setItemMeta(itemMeta);
			inventory.setItem(48, itemStack);
		}

		if(!gMusicMain.getConfigService().G_DISABLE_PLAYLIST && type != MenuType.RADIO) {
			itemStack = new ItemStack(Material.NOTE_BLOCK);
			itemMeta = itemStack.getItemMeta();
			itemMeta.setDisplayName(gMusicMain.getMessageService().getMessage(playSettings.getPlayListMode() == PlayListMode.DEFAULT ? "MusicGUI.music-playlist-mode-default" : playSettings.getPlayListMode() == PlayListMode.FAVORITES ? "MusicGUI.music-playlist-mode-favorites" : "MusicGUI.music-playlist-mode-radio"));
			itemStack.setItemMeta(itemMeta);
			inventory.setItem(49, itemStack);
		}

		if(!gMusicMain.getConfigService().G_DISABLE_OPTIONS) {
			itemStack = new ItemStack(Material.HOPPER);
			itemMeta = itemStack.getItemMeta();
			itemMeta.setDisplayName(gMusicMain.getMessageService().getMessage("MusicGUI.music-options"));
			itemStack.setItemMeta(itemMeta);
			inventory.setItem(50, itemStack);
		}

		if(!gMusicMain.getConfigService().G_DISABLE_SEARCH && playSettings.getPlayListMode() != PlayListMode.RADIO && gMusicMain.getVersionService().isAvailable()) {
			itemStack = new ItemStack(Material.OAK_SIGN);
			itemMeta = itemStack.getItemMeta();
			itemMeta.setDisplayName(searchKey == null || searchKey.isEmpty() ? gMusicMain.getMessageService().getMessage("MusicGUI.music-search-none") : gMusicMain.getMessageService().getMessage("MusicGUI.music-search", "%Search%", searchKey));
			itemStack.setItemMeta(itemMeta);
			inventory.setItem(51, itemStack);
		}

		setPauseResumeBar();
	}

	public void setPauseResumeBar() {
		if(optionState || playSettings.getPlayListMode() == PlayListMode.RADIO) return;

		PlayState songSettings = gMusicMain.getPlayService().getPlayState(uuid);

		if(songSettings != null) {
			ItemStack itemStack = new ItemStack(Material.END_CRYSTAL);
			ItemMeta itemMeta = itemStack.getItemMeta();
			if(songSettings.isPaused()) {
				itemMeta.setDisplayName(gMusicMain.getMessageService().getMessage("MusicGUI.music-resume"));
			} else {
				itemMeta.setDisplayName(gMusicMain.getMessageService().getMessage("MusicGUI.music-pause"));
			}
			itemStack.setItemMeta(itemMeta);
			inventory.setItem(45, itemStack);

			itemStack = new ItemStack(Material.BARRIER);
			itemMeta = itemStack.getItemMeta();
			itemMeta.setDisplayName(gMusicMain.getMessageService().getMessage("MusicGUI.music-stop"));
			itemStack.setItemMeta(itemMeta);
			inventory.setItem(46, itemStack);

			itemStack = new ItemStack(Material.FEATHER);
			itemMeta = itemStack.getItemMeta();
			itemMeta.setDisplayName(gMusicMain.getMessageService().getMessage("MusicGUI.music-skip"));
			itemStack.setItemMeta(itemMeta);
			inventory.setItem(47, itemStack);

			return;
		}

		inventory.setItem(45, null);
		inventory.setItem(46, null);
		inventory.setItem(47, null);
	}

	public void setOptionsBar() {
		optionState = true;

		clearBar();

		ItemStack itemStack = new ItemStack(Material.CHEST);
		ItemMeta itemMeta = itemStack.getItemMeta();
		itemMeta.setDisplayName(gMusicMain.getMessageService().getMessage("MusicGUI.music-back"));
		itemStack.setItemMeta(itemMeta);
		inventory.setItem(45, itemStack);
		if(type != MenuType.RADIO) {
			itemStack = new ItemStack(Material.MAGMA_CREAM);
			itemMeta = itemStack.getItemMeta();
			itemMeta.setDisplayName(gMusicMain.getMessageService().getMessage("MusicGUI.music-options-volume", "%Volume%", "" + playSettings.getVolume()));
			itemStack.setItemMeta(itemMeta);
			inventory.setItem(46, itemStack);
			itemStack = new ItemStack(Material.FIREWORK_ROCKET);
			itemMeta = itemStack.getItemMeta();
			itemMeta.setDisplayName(gMusicMain.getMessageService().getMessage("MusicGUI.music-options-particle", "%Particle%", gMusicMain.getMessageService().getMessage(playSettings.isShowingParticles() ? "MusicGUI.music-options-true" : "MusicGUI.music-options-false")));
			itemMeta.addItemFlags(ItemFlag.values());
			itemStack.setItemMeta(itemMeta);
			inventory.setItem(47, itemStack);
		}

		if(playSettings.getPlayListMode() != PlayListMode.RADIO) {
			if(type != MenuType.RADIO) {
				itemStack = new ItemStack(Material.DIAMOND);
				itemMeta = itemStack.getItemMeta();
				itemMeta.setDisplayName(gMusicMain.getMessageService().getMessage("MusicGUI.music-options-join", "%Join%", gMusicMain.getMessageService().getMessage(playSettings.isPlayOnJoin() ? "MusicGUI.music-options-true" : "MusicGUI.music-options-false")));
				itemStack.setItemMeta(itemMeta);
				inventory.setItem(48, itemStack);
				itemStack = new ItemStack(Material.BLAZE_POWDER);
				itemMeta = itemStack.getItemMeta();
				itemMeta.setDisplayName(gMusicMain.getMessageService().getMessage(playSettings.getPlayMode() == PlayMode.DEFAULT ? "MusicGUI.music-options-play-mode-once" : playSettings.getPlayMode() == PlayMode.SHUFFLE ? "MusicGUI.music-options-play-mode-shuffle" : "MusicGUI.music-options-play-mode-repeat"));
				itemStack.setItemMeta(itemMeta);
				inventory.setItem(49, itemStack);
			}
			itemStack = new ItemStack(Material.TOTEM_OF_UNDYING);
			itemMeta = itemStack.getItemMeta();
			itemMeta.setDisplayName(gMusicMain.getMessageService().getMessage("MusicGUI.music-options-reverse", "%Reverse%", gMusicMain.getMessageService().getMessage(playSettings.isReverseMode() ? "MusicGUI.music-options-true" : "MusicGUI.music-options-false")));
			itemStack.setItemMeta(itemMeta);
			inventory.setItem(50, itemStack);
		}

		if(type == MenuType.JUKEBOX) {
			itemStack = new ItemStack(Material.REDSTONE);
			itemMeta = itemStack.getItemMeta();
			itemMeta.setDisplayName(gMusicMain.getMessageService().getMessage("MusicGUI.music-options-range", "%Range%", "" + playSettings.getRange()));
			itemStack.setItemMeta(itemMeta);
			inventory.setItem(51, itemStack);
		}
	}

	public void setPage(int newPage) {
		List<Song> songs = new ArrayList<>();

		if(playSettings.getPlayListMode() != PlayListMode.RADIO) {
			songs = playSettings.getPlayListMode() == PlayListMode.FAVORITES ? playSettings.getFavorites() : gMusicMain.getSongService().getSongs();
			if(searchKey != null && !searchKey.isEmpty()) songs = gMusicMain.getSongService().filterSongsBySearch(songs, searchKey);
		}

		if(newPage > getMaxPageSize(songs.size())) newPage = getMaxPageSize(songs.size());
		if(newPage < 1) newPage = 1;

		page = newPage;

		for(int slot = 0; slot < 45; slot++) inventory.setItem(slot, null);

		pageSongs.clear();

		if(!songs.isEmpty()) {
			for(int songPosition = (page - 1) * 45; songPosition < 45 * page && songPosition < songs.size(); songPosition++) {
				Song song = songs.get(songPosition);
				ItemStack itemStack = new ItemStack(song.getDiscMaterial());
				ItemMeta itemMeta = itemStack.getItemMeta();
				itemMeta.setDisplayName(gMusicMain.getMessageService().getMessage(
						"MusicGUI.disc-title",
						"%Song%", song.getId(),
						"%SongTitle%", song.getTitle(),
						"%Author%", song.getAuthor().isEmpty() ? gMusicMain.getMessageService().getMessage("MusicGUI.disc-empty-author") : song.getAuthor(),
						"%OriginalAuthor%", song.getOriginalAuthor().isEmpty() ? gMusicMain.getMessageService().getMessage("MusicGUI.disc-empty-original-author") : song.getOriginalAuthor()
				));
				List<String> description = new ArrayList<>();
				for(String descriptionRow : song.getDescription()) description.add(gMusicMain.getMessageService().toFormattedMessage("&6" + descriptionRow));
				if(playSettings.getFavorites().contains(song)) description.add(gMusicMain.getMessageService().getMessage("MusicGUI.disc-favorite"));
				if(type == MenuType.DEFAULT && gMusicMain.getConfigService().DISC_BUY_ENABLED) description.addAll(gMusicMain.getMessageService().getMessageList("MusicGUI.disc-buy-lore", "%Price%", getFormattedBuyPrice()));
				itemMeta.setLore(description);
				pageSongs.put(songPosition % 45, song);
				itemMeta.addItemFlags(ItemFlag.values());
				itemStack.setItemMeta(itemMeta);
				inventory.setItem(songPosition % 45, itemStack);
			}
		}

		if(page > 1) {
			ItemStack itemStack = new ItemStack(Material.ARROW);
			ItemMeta itemMeta = itemStack.getItemMeta();
			itemMeta.setDisplayName(gMusicMain.getMessageService().getMessage("MusicGUI.last-page"));
			itemStack.setItemMeta(itemMeta);
			inventory.setItem(52, itemStack);
		} else {
			ItemStack itemStack = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
			ItemMeta itemMeta = itemStack.getItemMeta();
			itemMeta.setDisplayName(" ");
			itemStack.setItemMeta(itemMeta);
			inventory.setItem(52, itemStack);
		}

		if(page < getMaxPageSize(songs.size())) {
			ItemStack itemStack = new ItemStack(Material.ARROW);
			ItemMeta itemMeta = itemStack.getItemMeta();
			itemMeta.setDisplayName(gMusicMain.getMessageService().getMessage("MusicGUI.next-page"));
			itemStack.setItemMeta(itemMeta);
			inventory.setItem(53, itemStack);
		} else {
			ItemStack itemStack = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
			ItemMeta itemMeta = itemStack.getItemMeta();
			itemMeta.setDisplayName(" ");
			itemStack.setItemMeta(itemMeta);
			inventory.setItem(53, itemStack);
		}
	}

	private int getMaxPageSize(int songCount) { return (songCount / 45) + (songCount % 45 == 0 ? 0 : 1); }

	private String getFormattedBuyPrice() {
		double price = gMusicMain.getConfigService().DISC_BUY_PRICE;
		if(gMusicMain.getVaultLink() != null) return gMusicMain.getVaultLink().format(price);
		return "" + price;
	}

	private void buyDisc(@NotNull Player buyer, @NotNull Song song) {
		if(!gMusicMain.getPermissionService().hasPermission(buyer, "Music.Buy", "Music.*")) {
			gMusicMain.getMessageService().sendMessage(buyer, "Messages.command-gmusic-buy-permission-error");
			return;
		}

		if(gMusicMain.getVaultLink() == null) {
			gMusicMain.getMessageService().sendMessage(buyer, "Messages.command-gmusic-buy-economy-error");
			return;
		}

		double price = gMusicMain.getConfigService().DISC_BUY_PRICE;
		if(!gMusicMain.getVaultLink().has(buyer, price)) {
			gMusicMain.getMessageService().sendMessage(buyer, "Messages.command-gmusic-buy-funds-error", "%Price%", getFormattedBuyPrice());
			return;
		}

		if(!gMusicMain.getVaultLink().withdraw(buyer, price)) {
			gMusicMain.getMessageService().sendMessage(buyer, "Messages.command-gmusic-buy-economy-error");
			return;
		}

		ItemStack disc = gMusicMain.getDiscService().createDiscItem(song);
		for(ItemStack overflow : buyer.getInventory().addItem(disc).values()) buyer.getWorld().dropItem(buyer.getLocation(), overflow);
		gMusicMain.getMessageService().sendMessage(buyer, "Messages.command-gmusic-buy", "%Song%", song.getId(), "%SongTitle%", song.getTitle(), "%Price%", getFormattedBuyPrice());
	}

	public @NotNull UUID getOwner() { return uuid; }

	public @NotNull MenuType getMenuType() { return type; }

	public @NotNull PlaySettings getPlaySettings() { return playSettings; }

	public @NotNull Inventory getInventory() { return inventory; }

	public enum MenuType {

		DEFAULT,
		JUKEBOX,
		RADIO
    }

}
package com.oiha.lexikon.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.isxander.yacl.api.*;
import dev.isxander.yacl.gui.controllers.ActionController;
import dev.isxander.yacl.gui.controllers.BooleanController;
import dev.isxander.yacl.gui.controllers.ColorController;
import dev.isxander.yacl.gui.controllers.LabelController;
import dev.isxander.yacl.gui.controllers.cycling.CyclingListController;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.List;

import static com.oiha.lexikon.client.ModConfig.reloadDictionary;

public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parentScreen -> YetAnotherConfigLib.createBuilder()
                .title(Text.of("Spellchecker Configuration"))
                .category(ConfigCategory.createBuilder()
                        .name(Text.of("About"))
                        .option(Option.createBuilder(Text.class)
                                .name(Text.of("Information about the spellchecker"))
                                .binding(Binding.immutable(Text.literal("Hi, I'm Oiha, the developer of Lexikon.\n\nI hope you enjoy using Lexikon. If you need any help on how to use Lexikon, I have a video on youtube that explain in detail how to use it. \n\nIf you have any suggestions or feedback, please let me know. You can contact me on the community discord server or on my Github page. \n\nI want to really thank you for using Lexikon, this is a project that I have been working on for a long time and I'm really happy to see that people are using it. I'm not that good at moding but I'm trying my best to make mods that are useful and fun to use. \n\nSo if Lexikon helps you I would appreciate that you leave me a tip on my Ko-fi page. Also if you need a Minecraft server you can try BisectHosting with my link, that also helps me ;) \n\nThank you for using Lexikon!\n\n- Oiha\n\n")
                                        .append(Text.literal("[YouTube]")
                                                .setStyle(Style.EMPTY
                                                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://www.youtube.com/@Oiha_dev"))
                                                        .withColor(Formatting.RED).withBold(Boolean.TRUE).withUnderline(Boolean.TRUE)))
                                        .append(Text.literal(" "))
                                        .append(Text.literal("[Discord]")
                                                .setStyle(Style.EMPTY
                                                        .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://www.youtube.com/@Oiha_dev"))
                                                        .withColor(0x5662f6).withBold(Boolean.TRUE).withUnderline(Boolean.TRUE)))
                                        .append(Text.literal(" "))
                                        .append(Text.literal("[Github]")
                                                .setStyle(Style.EMPTY
                                                        .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://github.com/Oiha-dev"))
                                                        .withColor(Formatting.WHITE).withBold(Boolean.TRUE).withUnderline(Boolean.TRUE)))
                                        .append(Text.literal(" "))
                                        .append(Text.literal("[Ko-fi]")
                                                .setStyle(Style.EMPTY
                                                        .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://ko-fi.com/oiha_dev"))
                                                        .withColor(Formatting.LIGHT_PURPLE).withBold(Boolean.TRUE).withUnderline(Boolean.TRUE)))
                                        .append(Text.literal(" "))
                                        .append(Text.literal("[BisectHosting]")
                                                .setStyle(Style.EMPTY
                                                        .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://bisecthosting.com/oiha"))
                                                        .withColor(Formatting.AQUA).withBold(Boolean.TRUE).withUnderline(Boolean.TRUE)))))
                                .controller(LabelController::new)
                                .build())
                        .build())
                .category(ConfigCategory.createBuilder()
                        .name(Text.of("Esthetic Options"))
                        .group(OptionGroup.createBuilder()
                                .collapsed(true)
                                .name(Text.of("Suggestion Appearance"))
                                .option(Option.createBuilder(Color.class)
                                        .name(Text.of("Background color"))
                                        .binding(new Color(-536870912, true), () -> ModConfig.suggestionBackgroundColor, newValue -> ModConfig.suggestionBackgroundColor = newValue)
                                        .controller(opt -> new ColorController(opt, true))
                                        .build())
                                .option(Option.createBuilder(Color.class)
                                        .name(Text.of("Suggestions color"))
                                        .binding(Color.WHITE, () -> ModConfig.suggestionColor, newValue -> ModConfig.suggestionColor = newValue)
                                        .controller(ColorController::new)
                                        .build())
                                .option(Option.createBuilder(Color.class)
                                        .name(Text.of("Suggestions color when selected"))
                                        .binding(Color.YELLOW, () -> ModConfig.chosenSuggestionColor, newValue -> ModConfig.chosenSuggestionColor = newValue)
                                        .controller(ColorController::new)
                                        .build())
                                .build())
                        .group(OptionGroup.createBuilder()
                                .collapsed(true)
                                .name(Text.of("Icon Customization"))
                                .option(Option.createBuilder(Color.class)
                                        .name(Text.of("Icon color"))
                                        .binding(Color.WHITE, () -> ModConfig.dictionaryIconColor, newValue -> ModConfig.dictionaryIconColor = newValue)
                                        .controller(ColorController::new)
                                        .build())
                                .option(Option.createBuilder(String.class)
                                        .name(Text.of("Icon style"))
                                        .binding("book", () -> ModConfig.iconStyle, newValue -> ModConfig.iconStyle = newValue)
                                        .controller(opt -> new CyclingListController<>(opt, List.of("book", "floppydisk", "quill", "cd")))
                                        .build())
                                .build())
                        .group(OptionGroup.createBuilder()
                                .collapsed(true)
                                .name(Text.of("Outline Customization"))
                                .option(Option.createBuilder(boolean.class)
                                        .name(Text.of("Toggle the outline"))
                                        .binding(true, () -> ModConfig.outlineEnabled, newValue -> ModConfig.outlineEnabled = newValue)
                                        .controller(BooleanController::new)
                                        .build())
                                .option(Option.createBuilder(Color.class)
                                        .name(Text.of("Outline color"))
                                        .binding(Color.WHITE, () -> ModConfig.outlineColor, newValue -> ModConfig.outlineColor = newValue)
                                        .controller(ColorController::new)
                                        .build())
                                .build())
                        .group(OptionGroup.createBuilder()
                                .collapsed(true)
                                .name(Text.of("Underline Appearance"))
                                .option(Option.createBuilder(String.class)
                                        .name(Text.of("Underline style"))
                                        .binding("Straight", () -> ModConfig.underlineStyle, newValue -> ModConfig.underlineStyle = newValue)
                                        .controller(opt -> new CyclingListController<>(opt, List.of("Straight", "Wavy", "Dotted")))
                                        .build())
                                .option(Option.createBuilder(Color.class)
                                        .name(Text.of("Underline color"))
                                        .binding(Color.RED, () -> ModConfig.underlineColor, newValue -> ModConfig.underlineColor = newValue)
                                        .controller(ColorController::new)
                                        .build())
                                .option(Option.createBuilder(Color.class)
                                        .name(Text.of("Underline Minecraft color"))
                                        .tooltip(Text.of("This color is used when the word is part of Minecraft and has suggestions, which prevents confusion when a Minecraft name is also a real word that is misspelled"))
                                        .binding(new Color(0xFFAA00), () -> ModConfig.underineMinecraftColor, newValue -> ModConfig.underineMinecraftColor = newValue)
                                        .controller(ColorController::new)
                                        .build())
                                .build())
                        .build())
                .category(ConfigCategory.createBuilder()
                        .name(Text.of("Technical Options"))
                        .group(OptionGroup.createBuilder()
                                .collapsed(true)
                                .name(Text.of("Spellchecker Functionality"))
                                .option(Option.createBuilder(Text.class)
                                        .name(Text.of("Language"))
                                        .binding(Binding.immutable(Text.literal("If you change the language a lag spike may occur.")))
                                        .controller(LabelController::new)
                                        .build())
                                .option(Option.createBuilder(String.class)
                                        .name(Text.of("Language"))
                                        .binding("English (GB)", () -> ModConfig.currentLanguage, newValue -> ModConfig.currentLanguage = newValue)
                                        .controller(opt -> new CyclingListController<>(opt, List.of("English (GB)", "English (US)", "French", "Spanish", "German", "Italian", "Dutch", "Portuguese", "Catalan")))
                                        .build())
                                .option(Option.createBuilder(boolean.class)
                                        .name(Text.of("Toggle the spellchecker"))
                                        .tooltip(Text.of("This will enable or disable the entire spellchecker."))
                                        .binding(true, () -> ModConfig.spellcheckerEnabled, newValue -> ModConfig.spellcheckerEnabled = newValue)
                                        .controller(BooleanController::new)
                                        .build())
                                .option(Option.createBuilder(boolean.class)
                                        .name(Text.of("Check spelling in commands"))
                                        .tooltip(Text.of("This will enable or disable the spellchecker in commands, useful for servers owners that want to check the spelling of the text in there commands"))
                                        .binding(false, () -> ModConfig.spellcheckerInCommands, newValue -> ModConfig.spellcheckerInCommands = newValue)
                                        .controller(BooleanController::new)
                                        .build())
                                .build())
                        .group(OptionGroup.createBuilder()
                                .collapsed(true)
                                .name(Text.of("User Interface"))
                                .option(Option.createBuilder(boolean.class)
                                        .name(Text.of("Toggle the flag"))
                                        .tooltip(Text.of("This will enable or disable the flag that tell the current language of the spellchecker"))
                                        .binding(false, () -> ModConfig.flagButtonEnabled, newValue -> ModConfig.flagButtonEnabled = newValue)
                                        .controller(BooleanController::new)
                                        .build())
                                .option(Option.createBuilder(boolean.class)
                                        .tooltip(Text.of("This will enable or disable the dictionary, and the dictionary icon"))
                                        .name(Text.of("Toggle the dictionary"))
                                        .binding(true, () -> ModConfig.dictionaryEnabled, newValue -> ModConfig.dictionaryEnabled = newValue)
                                        .controller(BooleanController::new)
                                        .build())
                                .build())
                        .group(OptionGroup.createBuilder()
                                .collapsed(true)
                                .name(Text.of("Dictionary Options"))
                                .option(ButtonOption.createBuilder()
                                        .name(Text.of("Reload Minecraft Dictionary"))
                                        .tooltip(Text.of("This will reload the Minecraft dictionary, this is useful if you want to update the dictionary with the latest Minecraft names or if you added a new mod"))
                                        .action((yaclScreen, buttonOption) -> {
                                            reloadDictionary();
                                        })
                                        .controller(ActionController::new)
                                        .build())
                                .option(ButtonOption.createBuilder()
                                        .name(Text.of("Access Minecraft Dictionary"))
                                        .action((yaclScreen, buttonOption) -> {
                                            openFile("config/Lexikon/minecraftDictionary.txt");
                                        })
                                        .controller(ActionController::new)
                                        .build())
                                .option(ButtonOption.createBuilder()
                                        .name(Text.of("Access personal Dictionary"))
                                        .action((yaclScreen, buttonOption) -> {
                                            openFile("config/Lexikon/personalDictionary.txt");
                                        })
                                        .controller(ActionController::new)
                                        .build())
                                .build())
                        .build())
                .save(ModConfig::save)
                .build()
                .generateScreen(parentScreen);
    }

    private void openFile(String filePath) {
        try {
            File file = new File(filePath);
            String os = System.getProperty("os.name").toLowerCase();
            ProcessBuilder pb;

            if (os.contains("win")) {
                pb = new ProcessBuilder("notepad.exe", file.getAbsolutePath());
            } else if (os.contains("mac")) {
                pb = new ProcessBuilder("open", file.getAbsolutePath());
            } else if (os.contains("nix") || os.contains("nux")) {
                pb = new ProcessBuilder("xdg-open", file.getAbsolutePath());
            } else {
                throw new UnsupportedOperationException("Your OS is not supported.");
            }

            pb.start();
        } catch (IOException | UnsupportedOperationException e) {
            e.printStackTrace();
            // Handle the exception appropriately, perhaps by displaying an error message to the user
        }
    }
}
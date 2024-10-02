package com.oiha.lexikon.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.*;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.gui.controllers.cycling.CyclingListController;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import oshi.software.os.linux.LinuxOSFileStore;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.oiha.lexikon.client.ModConfig.reloadDictionary;
import static com.oiha.lexikon.client.ModConfig.spellcheckerEnabled;

public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parentScreen -> YetAnotherConfigLib.createBuilder()
                .title(Text.of("Used for narration. Could be used to render a title in the future."))
                .title(Text.of("Spellchecker Configuration"))
                .category(ConfigCategory.createBuilder()
                        .name(Text.of("About"))
                        .option(LabelOption.create(Text.literal("Hi, I'm Oiha, the developer of Lexikon.\n\nI hope you enjoy using Lexikon. If you need any help on how to use Lexikon, I have a video on youtube that explain in detail how to use it. \n\nIf you have any suggestions or feedback, please let me know. You can contact me on the community discord server or on my Github page. \n\nI want to really thank you for using Lexikon, this is a project that I have been working on for a long time and I'm really happy to see that people are using it. I'm not that good at moding but I'm trying my best to make mods that are useful and fun to use. \n\nSo if Lexikon helps you I would appreciate that you leave me a tip on my Ko-fi page. Also if you need a Minecraft server you can try BisectHosting with my link, that also helps me ;) \n\nThank you for using Lexikon!\n\n- Oiha\n\n")
                                .append(Text.literal("[YouTube]")
                                        .setStyle(Style.EMPTY
                                                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https:www.youtube.com/@Oiha_dev"))
                                                .withColor(Formatting.RED).withBold(Boolean.TRUE).withUnderline(Boolean.TRUE)))
                                .append(Text.literal(" "))
                                .append(Text.literal("[Discord]")
                                        .setStyle(Style.EMPTY
                                                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https:discord.gg/3D9TwmdPgh"))
                                                .withColor(0x5662f6).withBold(Boolean.TRUE).withUnderline(Boolean.TRUE)))
                                .append(Text.literal(" "))
                                .append(Text.literal("[Github]")
                                        .setStyle(Style.EMPTY
                                                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https:github.com/Oiha-dev/Lexikon"))
                                                .withColor(Formatting.WHITE).withBold(Boolean.TRUE).withUnderline(Boolean.TRUE)))
                                .append(Text.literal(" "))
                                .append(Text.literal("[Ko-fi]")
                                        .setStyle(Style.EMPTY
                                                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https:ko-fi.com/oiha_dev"))
                                                .withColor(Formatting.LIGHT_PURPLE).withBold(Boolean.TRUE).withUnderline(Boolean.TRUE)))
                                .append(Text.literal(" "))
                                .append(Text.literal("[BisectHosting]")
                                        .setStyle(Style.EMPTY
                                                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https:bisecthosting.com/oiha"))
                                                .withColor(Formatting.AQUA).withBold(Boolean.TRUE).withUnderline(Boolean.TRUE)))))
                        .build())
                .category(ConfigCategory.createBuilder()
                        .name(Text.of("Esthetic Options"))
                        .group(OptionGroup.createBuilder()
                                .collapsed(true)
                                .name(Text.of("Suggestion Appearance"))
                                .option(Option.createBuilder(Color.class)
                                        .name(Text.of("Background color"))
                                        .binding(new Color(-536870912, true), () -> ModConfig.suggestionBackgroundColor, newValue -> ModConfig.suggestionBackgroundColor = newValue)
                                        .controller(opt -> ColorControllerBuilder.create(opt)
                                                .allowAlpha(true))
                                        .description(OptionDescription.createBuilder()
                                                .text(Text.of("This color is used as the background of the suggestions."))
                                                .build())
                                        .build())
                                .option(Option.createBuilder(Color.class)
                                        .name(Text.of("Suggestions color"))
                                        .binding(Color.WHITE, () -> ModConfig.suggestionColor, newValue -> ModConfig.suggestionColor = newValue)
                                        .controller(ColorControllerBuilder::create)
                                        .description(OptionDescription.createBuilder()
                                                .text(Text.of("This color is used for the suggestions that are not selected."))
                                                .build())
                                        .build())
                                .option(Option.createBuilder(Color.class)
                                        .name(Text.of("Suggestions color when selected"))
                                        .binding(Color.YELLOW, () -> ModConfig.chosenSuggestionColor, newValue -> ModConfig.chosenSuggestionColor = newValue)
                                        .controller(ColorControllerBuilder::create)
                                        .description(OptionDescription.createBuilder()
                                                .text(Text.of("This color is used for the suggestion that is selected."))
                                                .build())
                                        .build())
                                .build())
                        .group(OptionGroup.createBuilder()
                                .collapsed(true)
                                .name(Text.of("Icon Customization"))
                                .option(Option.createBuilder(Color.class)
                                        .name(Text.of("Icon color"))
                                        .binding(Color.WHITE, () -> ModConfig.dictionaryIconColor, newValue -> ModConfig.dictionaryIconColor = newValue)
                                        .controller(ColorControllerBuilder::create)
                                        .description(OptionDescription.createBuilder()
                                                .text(Text.of("This color is used for the dictionary icon."))
                                                .build())
                                        .build())
                                .option(Option.createBuilder(String.class)
                                        .name(Text.of("Icon style"))
                                        .binding("book", () -> ModConfig.iconStyle, newValue -> ModConfig.iconStyle = newValue)
                                        .controller(opt -> CyclingListControllerBuilder.create(opt)
                                                .values(List.of("book", "floppydisk", "quill", "cd"))
                                                .valueFormatter(Text::literal))
                                        .description(OptionDescription.createBuilder()
                                                .text(Text.of("This is the you want for the dictionary icon."))
                                                .build())
                                        .build())
                                .build())
                        .group(OptionGroup.createBuilder()
                                .collapsed(true)
                                .name(Text.of("Outline Customization"))
                                .option(Option.createBuilder(boolean.class)
                                        .name(Text.of("Toggle the outline"))
                                        .binding(true, () -> ModConfig.outlineEnabled, newValue -> ModConfig.outlineEnabled = newValue)
                                        .controller(BooleanControllerBuilder::create)
                                        .build())
                                .option(Option.createBuilder(Color.class)
                                        .name(Text.of("Outline color"))
                                        .binding(Color.WHITE, () -> ModConfig.outlineColor, newValue -> ModConfig.outlineColor = newValue)
                                        .controller(ColorControllerBuilder::create)
                                        .description(OptionDescription.createBuilder()
                                                .text(Text.of("This color is used for the outline of the suggestions."))
                                                .build())
                                        .build())
                                .build())
                        .group(OptionGroup.createBuilder()
                                .collapsed(true)
                                .name(Text.of("Underline Appearance"))
                                .option(Option.createBuilder(String.class)
                                        .name(Text.of("Underline style"))
                                        .binding("Straight", () -> ModConfig.underlineStyle, newValue -> ModConfig.underlineStyle = newValue)
                                        .controller(opt -> CyclingListControllerBuilder.create(opt)
                                                .values(List.of("Straight", "Wavy", "Dotted", "Dashed"))
                                                .valueFormatter(Text::literal))
                                        .description(OptionDescription.createBuilder()
                                                .text(Text.of("This is the style of the underline."))
                                                .build())
                                        .build())
                                .option(Option.createBuilder(Color.class)
                                        .name(Text.of("Underline color"))
                                        .binding(Color.RED, () -> ModConfig.underlineColor, newValue -> ModConfig.underlineColor = newValue)
                                        .controller(ColorControllerBuilder::create)
                                        .description(OptionDescription.createBuilder()
                                                .text(Text.of("This color is used for the underline of the suggestions."))
                                                .build())
                                        .build())
                                .option(Option.createBuilder(Color.class)
                                        .name(Text.of("Underline Minecraft color"))
                                        .description(OptionDescription.of(Text.of("This color is used when the word is part of Minecraft and has suggestions, which prevents confusion when a Minecraft name is also a real word that is misspelled")))
                                        .binding(new Color(0xFFAA00), () -> ModConfig.underineMinecraftColor, newValue -> ModConfig.underineMinecraftColor = newValue)
                                        .controller(ColorControllerBuilder::create)
                                        .build())
                                .build())
                        .build())
                .category(ConfigCategory.createBuilder()
                        .name(Text.of("Technical Options"))
                        .group(OptionGroup.createBuilder()
                                .collapsed(true)
                                .name(Text.of("Spellchecker Functionality"))
                                .option(Option.createBuilder(String.class)
                                        .name(Text.of("Language"))
                                        .binding("English (GB)", () -> ModConfig.currentLanguage, newValue -> ModConfig.currentLanguage = newValue)
                                        .controller(opt -> CyclingListControllerBuilder.create(opt)
                                                .values(ModConfig.possibleLanguages)
                                                .valueFormatter(Text::literal))
                                        .description(OptionDescription.createBuilder()
                                                .text(Text.of("This is the language that the spellchecker will use."))
                                                .build())
                                        .build())
                                .option(Option.createBuilder(boolean.class)
                                        .name(Text.of("Toggle the spellchecker"))
                                        .description(OptionDescription.of(Text.of("This will enable or disable the entire spellchecker.")))
                                        .binding(true, () -> spellcheckerEnabled, newValue -> spellcheckerEnabled = newValue)
                                        .controller(BooleanControllerBuilder::create)
                                        .description(OptionDescription.createBuilder()
                                                .text(Text.of("This will enable or disable the spellchecker in the chat."))
                                                .build())
                                        .build())
                                .option(Option.createBuilder(boolean.class)
                                        .name(Text.of("Check spelling in commands"))
                                        .description(OptionDescription.of(Text.of("This will enable or disable the spellchecker in commands, useful for servers owners that want to check the spelling of the text in there commands")))
                                        .binding(false, () -> ModConfig.spellcheckerInCommands, newValue -> ModConfig.spellcheckerInCommands = newValue)
                                        .controller(BooleanControllerBuilder::create)
                                        .description(OptionDescription.createBuilder()
                                                .text(Text.of("This will enable or disable the spellchecker in commands."))
                                                .build())
                                        .build())
                                .build())
                        .group(OptionGroup.createBuilder()
                                .collapsed(true)
                                .name(Text.of("User Interface"))
                                .option(Option.createBuilder(boolean.class)
                                        .name(Text.of("Toggle the flag"))
                                        .description(OptionDescription.of(Text.of("This will enable or disable the icon of flag that tell the current language of the spellchecker")))
                                        .binding(false, () -> ModConfig.flagButtonEnabled, newValue -> ModConfig.flagButtonEnabled = newValue)
                                        .controller(BooleanControllerBuilder::create)
                                        .build())
                                .option(Option.createBuilder(boolean.class)
                                        .description(OptionDescription.of(Text.of("This will enable or disable the dictionary, and the dictionary icon")))
                                        .name(Text.of("Toggle the dictionary"))
                                        .binding(true, () -> ModConfig.dictionaryEnabled, newValue -> ModConfig.dictionaryEnabled = newValue)
                                        .controller(BooleanControllerBuilder::create)
                                        .build())
                                .build())
                        .group(OptionGroup.createBuilder()
                                .collapsed(true)
                                .name(Text.of("Dictionary Options"))
                                .option(ButtonOption.createBuilder()
                                        .name(Text.of("Reload Minecraft Dictionary"))
                                        .description(OptionDescription.of(Text.of("This will reload the Minecraft dictionary, this is useful if you want to update the dictionary with the latest Minecraft names or if you added a new mod")))
                                        .action((yaclScreen, buttonOption) -> {
                                            reloadDictionary();
                                        })
                                        .build())
                                .option(ButtonOption.createBuilder()
                                        .name(Text.of("Access Minecraft Dictionary"))
                                        .action((yaclScreen, buttonOption) -> openFile("config/Lexikon/minecraftDictionary.txt"))
                                        .build())
                                .option(ButtonOption.createBuilder()
                                        .name(Text.of("Access personal Dictionary"))
                                        .action((yaclScreen, buttonOption) -> openFile("config/Lexikon/personalDictionary.txt"))
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
             //Handle the exception appropriately, perhaps by displaying an error message to the user
        }
    }
}
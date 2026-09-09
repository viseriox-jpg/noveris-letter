package dev.noveris.letter.book;

import dev.noveris.letter.NoverisLetter;
import dev.noveris.letter.mail.MailLetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.Filterable;
import net.minecraft.world.item.component.WrittenBookContent;

import java.util.List;

/** Builds the physical representation of a letter without replacing the persisted source of truth. */
public final class LetterBookFactory {
    private LetterBookFactory() { }

    public static ItemStack create(MailLetter letter) {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        String title = letter.subject().isBlank() ? "Correspondência de " + letter.senderName() : letter.subject();
        String body = "Ao estimado " + letter.recipientName() + ",\n\n" + letter.content()
                + "\n\n— Enviado por " + letter.senderName() + "\nSob o selo postal de Noveris";
        book.set(DataComponents.CUSTOM_NAME, Component.literal(title));
        book.set(DataComponents.WRITTEN_BOOK, new WrittenBookContent(
                Filterable.passThrough(title), "Serviço Postal de Noveris", 0,
                List.of(Filterable.passThrough(Component.literal(body))), true));
        CompoundTag identity = new CompoundTag();
        identity.putString("letter_id", letter.id().toString());
        identity.putString("sender_id", letter.senderId().toString());
        identity.putString("recipient_id", letter.recipientId().toString());
        identity.putLong("sent_at", letter.sentAt());
        identity.putString("status", letter.status().name());
        identity.putInt("format_version", 1);
        book.set(DataComponents.CUSTOM_DATA, CustomData.of(identity));
        return book;
    }
}

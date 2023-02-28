// Copywrite Terry Hancock 2023
package host.thanco;

import javafx.scene.control.ListCell;

public class ChatItemViewCell extends ListCell<ChatItem> {
    @Override
    public void updateItem(ChatItem chatItem, boolean empty) {
        super.updateItem(chatItem, empty);
        if (chatItem != null && !empty) {
            ChatItemCell chatCell = new ChatItemCell();
            chatCell.setDetails(chatItem);
            setGraphic(chatCell.getHBox());
        }
    }
}

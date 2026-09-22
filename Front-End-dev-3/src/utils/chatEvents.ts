export const CHAT_READ_EVENT = "passro:chat-read";

export function notifyChatRead() {
    window.dispatchEvent(new Event(CHAT_READ_EVENT));
}

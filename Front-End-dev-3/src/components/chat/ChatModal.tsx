import blockIcon from "../../assets/icons/block.svg";
import noAlarmIcon from "../../assets/icons/noAlarm.svg";
import exitIcon from "../../assets/icons/exit.svg";

interface ChatModalProps {
    onClose: () => void;
    onReport: () => void;
    onExit: () => void;
    chatMessageId: number;
}

import { useNavigate } from "react-router-dom";
import { ReportIcon } from "../../assets/icons/report";

export default function ChatModal({ onClose, onExit, chatMessageId }: ChatModalProps) {
    const navigate = useNavigate();
    const handleReport = () => {
        onClose();
        navigate("/report", {
            state: {
                targetType: "CHAT_MESSAGE",
                chatMessageId,
            },
        });
    };

    return (
        <div
            onClick={onClose}
            className="fixed inset-0 z-[100] flex items-center justify-center bg-black/30"
        >
            <div
                onClick={(e) => e.stopPropagation()}
                className="w-full max-w-[320px] rounded-2xl bg-white p-5"
            >
                <div className="flex flex-col bg-gray-50 mb-5 rounded-xl">
                    <div className="flex gap-4 px-6 py-4 border-b border-gray-100">
                        <img src={blockIcon} />
                        <span className="flex text-gray-900">차단하기</span>
                    </div>
                    <button
                        onClick={handleReport}
                        className="flex gap-4 px-6 py-4 border-b border-gray-100"
                    >
                        <ReportIcon />
                        <span className="flex text-gray-900">신고하기</span>
                    </button>
                    <div className="flex gap-4 px-6 py-4 border-b border-gray-100">
                        <img src={noAlarmIcon} />
                        <span className="flex text-gray-900">알림끄기</span>
                    </div>
                    <button
                        type="button"
                        onClick={onExit}
                        className="flex gap-4 border-b border-gray-100 px-6 py-4 text-left"
                    >
                        <img src={exitIcon} alt="" />
                        <span className="flex text-errorRed">나가기</span>
                    </button>
                </div>
                <button
                    onClick={onClose}
                    className="flex w-full items-center justify-center bg-gray-100 rounded-[10px] py-2.5 text-gray-900 text-sm"
                >
                    닫기
                </button>
            </div>
        </div>
    );
}

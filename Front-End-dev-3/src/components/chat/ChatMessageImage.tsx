import { useEffect, useState } from "react";
import { createPortal } from "react-dom";
import { fileApi } from "../../apis";

interface ChatMessageImageProps {
    imageKey: string;
    alt: string;
    borderRadiusClassName?: string;
}

export default function ChatMessageImage({
    imageKey,
    alt,
    borderRadiusClassName = "rounded-[18px]",
}: ChatMessageImageProps) {
    const [imageUrl, setImageUrl] = useState<string | null>(null);
    const [hasError, setHasError] = useState(false);
    const [isPreviewOpen, setIsPreviewOpen] = useState(false);

    useEffect(() => {
        let isCanceled = false;

        setImageUrl(null);
        setHasError(false);

        void fileApi
            .getImageDownloadUrl(imageKey)
            .then((url) => {
                if (!isCanceled) {
                    setImageUrl(url);
                }
            })
            .catch(() => {
                if (!isCanceled) {
                    setHasError(true);
                }
            });

        return () => {
            isCanceled = true;
        };
    }, [imageKey]);

    useEffect(() => {
        if (!isPreviewOpen) {
            return;
        }

        const handleKeyDown = (event: KeyboardEvent) => {
            if (event.key === "Escape") {
                setIsPreviewOpen(false);
            }
        };

        window.addEventListener("keydown", handleKeyDown);
        return () => window.removeEventListener("keydown", handleKeyDown);
    }, [isPreviewOpen]);

    if (hasError) {
        return (
            <div
                className={`flex h-28 w-44 items-center justify-center bg-gray-100 px-4 text-center text-xs font-medium text-gray-400 ${borderRadiusClassName}`}
                role="img"
                aria-label={alt}
            >
                이미지를 불러오지 못했습니다.
            </div>
        );
    }

    if (!imageUrl) {
        return (
            <div
                className={`h-28 w-44 animate-pulse bg-gray-100 ${borderRadiusClassName}`}
                aria-label="채팅 이미지 불러오는 중"
                role="status"
            />
        );
    }

    return (
        <>
            <button
                type="button"
                onClick={() => setIsPreviewOpen(true)}
                className={`block max-w-full overflow-hidden focus:outline-none focus-visible:ring-2 focus-visible:ring-purple-500 ${borderRadiusClassName}`}
                aria-label={`${alt} 크게 보기`}
            >
                <img
                    src={imageUrl}
                    alt={alt}
                    className="block max-h-[280px] max-w-full object-cover transition-transform active:scale-95"
                />
            </button>

            {isPreviewOpen
                ? createPortal(
                      <div
                          className="fixed inset-0 z-[100] mx-auto flex w-full max-w-[402px] items-center justify-center bg-black/80 p-4"
                          role="dialog"
                          aria-modal="true"
                          aria-label="채팅 이미지 미리보기"
                          onClick={() => setIsPreviewOpen(false)}
                      >
                          <button
                              type="button"
                              className="absolute right-4 top-[max(1rem,env(safe-area-inset-top))] flex h-10 w-10 items-center justify-center rounded-full bg-black/50 text-3xl text-white"
                              onClick={() => setIsPreviewOpen(false)}
                              aria-label="이미지 미리보기 닫기"
                          >
                              ×
                          </button>
                          <img
                              src={imageUrl}
                              alt={`${alt} 크게 보기`}
                              className="max-h-[calc(100dvh-5rem)] max-w-full object-contain"
                              onClick={(event) => event.stopPropagation()}
                          />
                      </div>,
                      document.body,
                  )
                : null}
        </>
    );
}

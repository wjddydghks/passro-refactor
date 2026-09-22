export default function LoadingPage() {
    return (
        <main
            className="flex min-h-dvh w-full flex-col items-center justify-center bg-white px-6"
            aria-busy="true"
            aria-labelledby="loading-title"
        >
            <img
                src="/Logo.png"
                alt="Passro"
                width={160}
                height={160}
                className="h-40 w-40 animate-pulse object-contain"
            />
            <h1
                id="loading-title"
                className="mt-5 text-lg font-bold text-gray-900"
            >
                화면을 불러오고 있어요
            </h1>
            <p className="mt-2 text-sm font-medium text-gray-500">
                잠시만 기다려주세요.
            </p>
            <span className="mt-6 h-8 w-8 animate-spin rounded-full border-4 border-purple-100 border-t-purple-500" />
        </main>
    );
}

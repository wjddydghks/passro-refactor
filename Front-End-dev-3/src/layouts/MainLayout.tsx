import { Outlet, useLocation } from "react-router-dom";
import Navbar from "../components/navbar/Navbar";

export default function MainLayout() {
  const { pathname } = useLocation();
  const isChatRoom = pathname.startsWith("/delivery/chat/");
  const hasInternalScroll = isChatRoom || pathname === "/home";

  return (
    <div className="isolate grid h-full min-h-0 w-full grid-rows-[minmax(0,1fr)_auto] overflow-hidden">
      <main
        className={`scrollbar-hidden relative min-h-0 overflow-x-hidden [&_.page-container]:min-h-full ${
          hasInternalScroll
            ? "overflow-y-hidden overscroll-none"
            : "overflow-y-auto"
        }`}
      >
        <Outlet />
      </main>
      <Navbar />
    </div>
  );
}

export const imgproxied = (url: string, width?: number, ext?: string) => {
    if (!ext) ext = "webp";
    const rs_option = width ? `rs:fit:${width}:0/` : "";

    return `https://imgproxy.suplitter.com/unsafe/${rs_option}plain/${encodeURIComponent(url)}@${ext}`
}
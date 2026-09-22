type SectionTitleProps = {
    children: string;
    accent?: boolean;
};

export function SectionTitle({ children, accent = false }: SectionTitleProps) {
    return (
        <h2
            className={`text-[15px] font-bold tracking-normal ${
                accent ? "text-purple-600" : "text-gray-900"
            }`}
        >
            {children}
        </h2>
    );
}

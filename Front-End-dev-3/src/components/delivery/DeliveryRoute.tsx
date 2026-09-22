interface DeliveryRouteProps {
    departure: string;
    destination: string;
}

export const DeliveryRoute = ({
    departure,
    destination,
}: DeliveryRouteProps) => {
    return (
        <div className="mt-10 flex w-full gap-2 px-5">
            <div className="flex flex-col items-center gap-2">
                <div className="h-4 w-4 rounded-full bg-gray-800 mt-1" />
                <span className="text-sm text-gray-600">{departure}</span>
            </div>
            <div className="mt-2.5 h-0.5 flex-1 bg-gray-800" />
            <div className="flex flex-col items-center gap-2">
                <div className="flex items-center justify-center">
                    <div className="h-6 w-6 rounded-full bg-purple-100" />
                    <div className="absolute h-2.5 w-2.5 rounded-full bg-purple-500" />
                </div>
                <span className="text-sm text-purple-600">{destination}</span>
            </div>
        </div>
    );
};

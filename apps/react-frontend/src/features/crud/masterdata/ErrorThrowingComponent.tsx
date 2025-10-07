
export const ErrorThrowingComponent = ({
    error,
}: {
    error?: Error | null;
}) => {
    if (error) {
        throw error;
    }

    return (
        <></>
    );
};
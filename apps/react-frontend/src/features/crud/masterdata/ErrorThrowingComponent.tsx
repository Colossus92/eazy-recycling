
export const ErrorThrowingComponent = ({
    error,
}: {
    error?: Error | null;
}) => {
    console.log(JSON.stringify(error))
    if (error) {
        throw error;
    }

    return (
        <></>
    );
};
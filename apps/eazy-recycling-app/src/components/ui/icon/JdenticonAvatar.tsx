import { toSvg } from 'jdenticon';

export const JdenticonAvatar = ({
  value,
  size = 64,
}: {
  value: string;
  size?: number;
}) => {
  const svg = toSvg(value, size);
  return <span dangerouslySetInnerHTML={{ __html: svg }} />;
};

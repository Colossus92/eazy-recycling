import LogoIcon from '@/assets/MediumLogo.svg';

export const Logo = () => {
  return (
    <>
      <img src={LogoIcon} alt="Logo" className="w-8 h-8" />
      <span className="text-color-brand-primary font-inter text-base font-semibold leading-6">
        Eazy Recycling
      </span>
    </>
  );
};

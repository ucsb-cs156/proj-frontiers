import { FaGear, FaGithub } from "react-icons/fa6";

export default function GithubSettingIcon({
  size = 24,
  gearColor = "blue",
  githubColor = "black",
  "data-testid": dataTestId = "GithubSettingIcon",
}) {
  return (
    <span
      style={{ display: "absolute", alignItems: "inline-block" }}
      data-testid={dataTestId}
    >
      <FaGithub
        size={size}
        color={githubColor}
        data-testid={`${dataTestId}-github-icon`}
      />
      <FaGear
        size={size / 1.5}
        color={gearColor}
        data-testid={`${dataTestId}-settings-icon`}
        style={{
          position: "relative",
          top: 0,
          left: 0,
          transform: "translate(-15%, 60%)",
        }}
      />
    </span>
  );
}

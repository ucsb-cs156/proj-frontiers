import { FaGear, FaGithub } from "react-icons/fa6";

export default function GithubSettingIcon({
  size = 24,
  gearColor = "blue",
  githubColor = "black",
  "data-testid": dataTestId = "GithubSettingIcon",
}) {
  return (
    <span
      style={{ position: "relative", display: "inline-block", marginRight: "12px" }}
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
          position: "absolute",
          bottom: 0,
          right: 0,
          transform: "translate(65%, 20%)",
        }}
      />
    </span>
  );
}

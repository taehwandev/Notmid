# Design System

This page records Notmid design tokens and exported component names. Reusable
Compose UI architecture and general frontend rules live in Tao Agent OS and
project skills.

## Core Rule

Feature and app modules should use `Notmid*` components, not raw Material3 components.

Material3 is the internal implementation detail of `:core:designsystem`.

## Theme

Use:

```kotlin
notmidTheme {
    NotmidTheme.colors
    NotmidTheme.typography
    NotmidTheme.spacing
    NotmidTheme.shapes
    NotmidTheme.elevation
}
```

`dynamicColor` defaults to `false` so Android Material You does not override product identity.

## Tokens

Base colors:

```text
Ink #101114
Slate #2D333B
Muted #575D64
Subtle #6B7178
Mist #F4F6F8
Warm Mist #F3F1EC
Cloud #FFFFFF
Line rgba(16,17,20,0.10)
```

Accents:

```text
Signal Green #14B87A
Route Blue #2F6BFF
Warm Clip #FF704D
Night Violet #7A4EF3
Alert Red #F04452
```

Typography:

```text
Display 34/38 700
Title 22/28 700
Headline 18/20 600
Body 15/22 400
BodySmall 13/18 400
Label 12/16 600
Caption 11/14 500
```

Shapes:

```text
control 8
card 16
cardLarge 24
sheet 28
pill 999
```

## Material3 Wrappers

Use these from `:core:designsystem`:

```text
NotmidText
NotmidTextField
NotmidButton
NotmidOutlinedButton
NotmidTextButton
NotmidIconButton
NotmidCheckbox
NotmidSwitch
NotmidScaffold
NotmidTopAppBar
NotmidCenterAlignedTopAppBar
NotmidLargeTopAppBar
NotmidSnackbarHost
NotmidFloatingActionButton
NotmidSurface
NotmidCard
NotmidHorizontalDivider
```

## Product Components

```text
NotmidBottomNavigation
NotmidGlassSurface
NotmidMetricTile
NotmidPillButton
NotmidActionRail
NotmidSectionHeader
NotmidGradientSummaryCard
NotmidGradientHeroCard
```

Feature adapters:

```text
NotmidClipCard -> NotmidGradientSummaryCard
NotmidPlaceCard -> NotmidGradientHeroCard
```

## UI Rules

- Do not create one-off colors/typography in feature screens when tokens exist.
- Keep media-derived palettes in UI models; those are product data.
- Keep `core:designsystem` free of destinations, routes, repositories, or product fake data.
- Do not use nested cards.
- Use glass only for functional surfaces.
- Fixed-format UI should have stable dimensions.
